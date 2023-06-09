package dev.polluxus.scryfall_sql.scryfall.converter;

import dev.polluxus.scryfall_sql.model.Card;
import dev.polluxus.scryfall_sql.model.Card.CardFace;
import dev.polluxus.scryfall_sql.model.enums.Colour;
import dev.polluxus.scryfall_sql.model.enums.Format;
import dev.polluxus.scryfall_sql.model.enums.Game;
import dev.polluxus.scryfall_sql.scryfall.model.ScryfallCard;
import dev.polluxus.scryfall_sql.util.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ScryfallCardCardConverter implements Converter<ScryfallCard, Card> {

    private final ScryfallCardCardFaceConverter cardToCardFace = new ScryfallCardCardFaceConverter();
    private final ScryfallCardFaceCardFaceConverter cardFaceToCardFace = new ScryfallCardFaceCardFaceConverter();

    @Override
    public Card convert(ScryfallCard source) {

        final UUID cardId = source.oracleId();

        final String name = source.name();

        final Set<Format> formats = source.legalities()
                .entrySet()
                .stream()
                .filter(e -> e.getValue().equals("legal"))
                .map(e -> Format.get(e.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        final List<CardFace> faces;

        // Do we have card faces? If so, get *most* card info from there.
        if (source.cardFaces() != null) {

            faces = source.cardFaces()
                    .stream()
                    .map(f -> cardFaceToCardFace.convert(Pair.of(source, f)))
                    .toList();
        } else {

            faces = List.of(cardToCardFace.convert(source));
        }

        final Set<Colour> colours = StringUtils.parseColours(source.colorIdentity());

        final Set<String> keywords = Set.copyOf(source.keywords());

        return new Card(
            cardId,
            name,
            faces,
            formats,
            colours,
            keywords
        );
    }
}
