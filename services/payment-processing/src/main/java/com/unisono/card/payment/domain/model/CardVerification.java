package com.unisono.card.payment.domain.model;

/**
 * Cardholder verification data entered for a payment. Never stored.
 *
 * @param pin PIN encrypted by the terminal (cash advance); card-data decrypts and verifies it
 * @param cvv CVV (card-not-present payments: ONLINE and MANUAL); verified against card-data
 */
public record CardVerification(PinBlock pin, String cvv) {

    public static CardVerification none() {
        return new CardVerification(null, null);
    }

    public static CardVerification ofPin(PinBlock pin) {
        return new CardVerification(pin, null);
    }

    public static CardVerification ofCvv(String cvv) {
        return new CardVerification(null, cvv);
    }

    public boolean hasPin() {
        return pin != null;
    }

    public boolean hasCvv() {
        return cvv != null && !cvv.isBlank();
    }

    @Override
    public String toString() {
        return String.format("CardVerification[pin=%s, cvv=%s]", hasPin() ? "***" : "-", hasCvv() ? "***" : "-");
    }
}
