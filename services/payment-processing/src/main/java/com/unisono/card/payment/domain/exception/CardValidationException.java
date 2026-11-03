package com.unisono.card.payment.domain.exception;

import com.unisono.card.shared.domain.valueobject.CardId;

/**
 * Exception thrown when card validation fails
 */
public class CardValidationException extends PaymentException {

    private final CardId cardId;
    private final String validationType;

    public CardValidationException(String message) {
        super(message);
        this.cardId = null;
        this.validationType = null;
    }

    public CardValidationException(CardId cardId, String validationType, String message) {
        super(String.format("Card validation failed for %s - %s: %s", cardId, validationType, message));
        this.cardId = cardId;
        this.validationType = validationType;
    }

    public static CardValidationException notFound(CardId cardId) {
        return new CardValidationException(cardId, "EXISTENCE", "Card not found");
    }

    public static CardValidationException expired(CardId cardId) {
        return new CardValidationException(cardId, "EXPIRATION", "Card is expired");
    }

    public static CardValidationException blocked(CardId cardId) {
        return new CardValidationException(cardId, "STATUS", "Card is blocked");
    }

    public static CardValidationException invalidCVV(CardId cardId) {
        return new CardValidationException(cardId, "CVV", "Invalid CVV");
    }

    public static CardValidationException invalidStatus(CardId cardId, String status) {
        return new CardValidationException(cardId, "STATUS",
                String.format("Card status '%s' is not valid for transactions", status));
    }

    public CardId getCardId() {
        return cardId;
    }

    public String getValidationType() {
        return validationType;
    }
}
