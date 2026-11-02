package com.unisono.card.payment.domain.model;

/**
 * Enum representing different payment methods/channels
 */
public enum PaymentMethod {

    /**
     * Chip card (EMV)
     */
    CHIP("Chip EMV", true),

    /**
     * Magnetic stripe swipe
     */
    SWIPE("Banda Magnética", false),

    /**
     * Contactless (NFC)
     */
    CONTACTLESS("Contactless NFC", true),

    /**
     * Card-not-present (online/ecommerce)
     */
    ONLINE("Compra Online", false),

    /**
     * Mobile wallet (Apple Pay, Google Pay, etc.)
     */
    MOBILE_WALLET("Billetera Móvil", true),

    /**
     * Manual key entry
     */
    MANUAL("Ingreso Manual", false);

    private final String displayName;
    private final boolean isSecure; // EMV chip, NFC are more secure

    PaymentMethod(String displayName, boolean isSecure) {
        this.displayName = displayName;
        this.isSecure = isSecure;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isSecure() {
        return isSecure;
    }

    public boolean requiresCVV() {
        return this == ONLINE || this == MANUAL;
    }

    public boolean isCardPresent() {
        return this != ONLINE;
    }
}
