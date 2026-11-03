package com.unisono.card.payment.domain.model;

/**
 * Cardholder verification data entered for a payment. Never stored.
 *
 * @param pin PIN (cash advance); verified against card-data
 * @param cvv CVV (card-not-present payments: ONLINE and MANUAL); verified against card-data
 */
public record CardVerification(String pin, String cvv) {

    public static CardVerification none() {
        return new CardVerification(null, null);
    }

    public static CardVerification ofPin(String pin) {
        return new CardVerification(pin, null);
    }

    public static CardVerification ofCvv(String cvv) {
        return new CardVerification(null, cvv);
    }

    public boolean hasPin() {
        return pin != null && !pin.isBlank();
    }

    public boolean hasCvv() {
        return cvv != null && !cvv.isBlank();
    }

    @Override
    public String toString() {
        return String.format("CardVerification[pin=%s, cvv=%s]", hasPin() ? "***" : "-", hasCvv() ? "***" : "-");
    }
}
