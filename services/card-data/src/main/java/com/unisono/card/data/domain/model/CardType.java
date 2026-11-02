package com.unisono.card.data.domain.model;

import lombok.Getter;

/**
 * Tipo de tarjeta según la red de pago
 */
@Getter
public enum CardType {
    VISA("Visa", "^4[0-9]{12}(?:[0-9]{3})?$"),
    MASTERCARD("Mastercard", "^5[1-5][0-9]{14}$"),
    AMEX("American Express", "^3[47][0-9]{13}$"),
    CABAL("Cabal", "^(6042|6043|5896)[0-9]{12}$"),  // Argentina
    NARANJA("Naranja", "^(5895|5218|5234)[0-9]{12}$"); // Argentina

    private final String displayName;
    private final String binPattern;

    CardType(String displayName, String binPattern) {
        this.displayName = displayName;
        this.binPattern = binPattern;
    }

    /**
     * Detecta el tipo de tarjeta a partir del número
     */
    public static CardType detectFromNumber(String cardNumber) {
        for (CardType type : values()) {
            if (cardNumber.matches(type.binPattern)) {
                return type;
            }
        }
        return VISA; // Por defecto
    }
}
