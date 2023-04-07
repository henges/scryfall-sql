package dev.polluxus.scryfall_projects.scryfall.converter;

import dev.polluxus.scryfall_projects.model.Card.CardFace;
import dev.polluxus.scryfall_projects.model.enums.Colour;
import dev.polluxus.scryfall_projects.scryfall.model.ScryfallCard;
import dev.polluxus.scryfall_projects.scryfall.model.ScryfallCard.ScryfallCardFace;
import dev.polluxus.scryfall_projects.util.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ScryfallCardFaceCardFaceConverter implements Converter<Pair<ScryfallCard, ScryfallCardFace>, CardFace> {

    @Override
    public CardFace convert(Pair<ScryfallCard, ScryfallCardFace> source) {

        final ScryfallCard card = source.getLeft();
        final ScryfallCardFace face = source.getRight();

        final UUID cardId = card.oracleId();
        final String name = face.name();
        final int manaValue = card.cmc();
        final List<String> manaCost = StringUtils.parseManaCost(face.manaCost());
        final List<String> cardTypes = StringUtils.parseCardTypes(face.typeLine());
        final String oracleText = face.oracleText();
        final String power = face.power();
        final String toughness = face.toughness();
        final String loyalty = face.loyalty();

        final Set<Colour> colourIdentity = StringUtils.parseColourIdentity(manaCost);

        return new CardFace(
                cardId, name, manaValue, manaCost, cardTypes, oracleText, power, toughness, loyalty, colourIdentity
        );
    }
}