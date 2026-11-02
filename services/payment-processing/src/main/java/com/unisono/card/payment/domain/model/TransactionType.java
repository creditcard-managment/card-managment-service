package com.unisono.card.payment.domain.model;

/**
 * Enum representing different types of card transactions
 */
public enum TransactionType {

    /**
     * Purchase at point of sale or online
     */
    PURCHASE("Compra", "P"),

    /**
     * Refund/credit to cardholder
     */
    REFUND("Devolución", "R"),

    /**
     * Reversal of previous transaction
     */
    REVERSAL("Reversa", "V"),

    /**
     * Cash advance from ATM
     */
    CASH_ADVANCE("Adelanto en Efectivo", "A"),

    /**
     * Pre-authorization (hold funds)
     */
    PRE_AUTHORIZATION("Pre-autorización", "PA"),

    /**
     * Capture of pre-authorized funds
     */
    CAPTURE("Captura", "C");

    private final String displayName;
    private final String code;

    TransactionType(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }

    public boolean isDebit() {
        return this == PURCHASE || this == CASH_ADVANCE || this == CAPTURE;
    }

    public boolean isCredit() {
        return this == REFUND || this == REVERSAL;
    }

    public boolean requiresAuthorization() {
        return this != REVERSAL; // Reversals don't need authorization
    }
}
