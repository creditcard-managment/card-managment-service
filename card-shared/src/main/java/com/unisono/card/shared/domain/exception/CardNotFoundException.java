package com.unisono.card.shared.domain.exception;

import com.unisono.card.shared.domain.valueobject.CardId;

public class CardNotFoundException extends RuntimeException {

    public CardNotFoundException(CardId cardId) {
        super(String.format("Card with ID %s not found", cardId.getValue().toString()));
    }

    public CardNotFoundException(String cardId) {
        super(String.format("Card with ID %s not found", cardId));
    }
}