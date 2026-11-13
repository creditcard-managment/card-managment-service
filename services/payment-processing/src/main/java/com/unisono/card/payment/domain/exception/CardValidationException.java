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

    public static CardValidationException cvvRequired(CardId cardId) {
        return new CardValidationException(cardId, "CVV", "CVV is required for card-not-present payments");
    }

    /**
     * card-data could not decrypt the PIN block (unknown key, wrong format or another card's block)
     */
    public static CardValidationException invalidPinBlock(CardId cardId) {
        return new CardValidationException(cardId, "PIN", "Invalid PIN block");
    }

    public static CardValidationException invalidPin(CardId cardId) {
        return new CardValidationException(cardId, "PIN", "Incorrect PIN");
    }

    public static CardValidationException pinRequired(CardId cardId) {
        return new CardValidationException(cardId, "PIN", "PIN is required");
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
