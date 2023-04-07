package dev.polluxus.scryfall_projects.processor;

import dev.polluxus.scryfall_projects.model.Card;
import dev.polluxus.scryfall_projects.model.Card.CardEdition;
import dev.polluxus.scryfall_projects.model.Card.CardFace;
import dev.polluxus.scryfall_projects.model.MagicSet;
import dev.polluxus.scryfall_projects.model.enums.Format;
import dev.polluxus.scryfall_projects.scryfall.converter.ScryfallCardCardConverter;
import dev.polluxus.scryfall_projects.scryfall.converter.ScryfallCardCardEditionConverter;
import dev.polluxus.scryfall_projects.scryfall.converter.ScryfallCardMagicSetConverter;
import dev.polluxus.scryfall_projects.scryfall.model.ScryfallCard;
import dev.polluxus.scryfall_projects.util.StringUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ScryfallSqlProcessor implements Processor<ScryfallCard> {

    private final ConcurrentHashMap<UUID, Boolean> processedOracleIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> processedSets = new ConcurrentHashMap<>();
    private final ScryfallCardCardConverter cardConverter = new ScryfallCardCardConverter();
    private final ScryfallCardCardEditionConverter editionConverter = new ScryfallCardCardEditionConverter();
    private final ScryfallCardMagicSetConverter setConverter = new ScryfallCardMagicSetConverter();

    private final Deque<String> setUpserts = new ConcurrentLinkedDeque<>();
    private final Deque<String> cardUpserts = new ConcurrentLinkedDeque<>();
    private final Deque<String> editionUpserts = new ConcurrentLinkedDeque<>();

    @Override
    public void commit(Writer writer) {

        writeString(writer, "BEGIN TRANSACTION;\n");

        setUpserts.forEach(s -> writeString(writer, s));
        cardUpserts.forEach(s -> writeString(writer, s));
        editionUpserts.forEach(s -> writeString(writer, s));

        writeString(writer, "END TRANSACTION;\n");
    }

    @Override
    public void process(List<ScryfallCard> elements, Writer writer) {

        for (var e : elements) {

            if (!Format.hasValidFormat(e.legalities()) || e.games().isEmpty()) {
                System.out.println("Skipping invalid card " + e.name());
                continue;
            }

            // Avoid processing oracle IDs and sets we've already processed.
            boolean setExists = processedSets.putIfAbsent(e.set(), true) != null;
            if (!setExists) {
                final MagicSet set = setConverter.convert(e);
                setUpserts.add(getSetUpsertSql(set));
            }
            boolean cardExists = processedOracleIds.putIfAbsent(e.oracleId(), true) != null;
            if (!cardExists) {
                final Card card = cardConverter.convert(e);
                cardUpserts.add(getCardUpsertSql(card));
            }
            final CardEdition edition = editionConverter.convert(e);
            editionUpserts.add(getEditionUpsertSql(edition));
        }
    }

    private static final String SET_UPSERT_STATEMENT = """
            INSERT INTO scryfall.set(code, name, release_date) VALUES ('$1', E'$2', '$3') ON CONFLICT DO NOTHING;
            """.trim();

    private static String getSetUpsertSql(MagicSet set) {

        final String sql = SET_UPSERT_STATEMENT
                .replace("$1", set.code())
                .replace("$2", StringUtils.escapeQuotes(set.name()))
                .replace("$3", set.releaseDate().toString());

        return sql + "\n";
    }

    private static final String CARD_UPSERT_STATEMENT = """
            INSERT INTO scryfall.card(id, name, formats, games)
            VALUES (
                '$1',
                E'$2',
                ARRAY[$3]::scryfall.format[],
                ARRAY[$4]::scryfall.game[]
            ) ON CONFLICT DO NOTHING;
            """.trim().replaceAll("\\s+", " ");

    private static final String CARD_FACE_UPSERT_STATEMENT = """
            INSERT INTO scryfall.card_face(
                card_id, name, mana_value, mana_cost, card_types, oracle_text, power, toughness, loyalty, colour_identity
            )
            VALUES(
                '$01', E'$02', $03, ARRAY[$04], ARRAY[$05], E'$06', $07, $08, $09, ARRAY[$10]::scryfall.colour[]
            ) ON CONFLICT DO NOTHING;
            """.trim().replaceAll("\\s+", " ");

    private static String getCardUpsertSql(Card card) {

        final StringBuilder sb = new StringBuilder();

        sb.append(CARD_UPSERT_STATEMENT
                .replace("$1", card.id().toString())
                .replace("$2", StringUtils.escapeQuotes(card.name()))
                .replace("$3", StringUtils.delimitedString(card.formats()))
                .replace("$4", StringUtils.delimitedString(card.games())));

        for (CardFace cf : card.faces()) {

            sb.append("\n");

            sb.append(CARD_FACE_UPSERT_STATEMENT
                    .replace("$01", cf.cardId().toString())
                    .replace("$02", StringUtils.escapeQuotes(cf.name()))
                    .replace("$03", String.format("%d", cf.manaValue()))
                    .replace("$04", StringUtils.delimitedString(cf.manaCost()))
                    .replace("$05", StringUtils.delimitedString(cf.cardTypes(), true))
                    .replace("$06", StringUtils.escapeQuotes(cf.oracleText()))
                    .replace("$07", StringUtils.quotedStringOrNullLiteral(cf.power()))
                    .replace("$08", StringUtils.quotedStringOrNullLiteral(cf.toughness()))
                    .replace("$09", StringUtils.quotedStringOrNullLiteral(cf.loyalty()))
                    .replace("$10", StringUtils.delimitedString(cf.colourIdentity())));
        }

        return sb + "\n";
    }

    private static final String EDITION_UPSERT_STATEMENT = """
            INSERT INTO scryfall.card_edition(id, card_id, set_code, collector_number, rarity, is_reprint, scryfall_url)
            VALUES ('$1', '$2', '$3', '$4', '$5'::scryfall.rarity, $6, '$7')
            ON CONFLICT DO NOTHING;
            """.trim().replaceAll("\\s+", " ");

    private static String getEditionUpsertSql(CardEdition ed) {

        final String sql = EDITION_UPSERT_STATEMENT
                .replace("$1", ed.id().toString())
                .replace("$2", ed.cardId().toString())
                .replace("$3", ed.setCode())
                .replace("$4", ed.collectorNumber())
                .replace("$5", ed.rarity().toString())
                .replace("$6", String.format("%b", ed.isReprint()))
                .replace("$7", ed.scryfallUrl());

        return sql + "\n";
    }

    private static void writeString(Writer writer, String string) {

        try {
            writer.write(string);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
