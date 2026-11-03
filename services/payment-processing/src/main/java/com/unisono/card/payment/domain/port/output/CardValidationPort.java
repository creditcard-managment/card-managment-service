package com.unisono.card.payment.domain.port.output;

import com.unisono.card.shared.domain.valueobject.CardId;

import java.util.Optional;

/**
 * Port for validating card information
 */
public interface CardValidationPort {

    /**
     * Current status of the card in card-data, or empty if the card does not exist
     */
    Optional<CardStatusSnapshot> findCardStatus(CardId cardId);

    /**
     * Verifies the PIN against card-data. A wrong PIN counts as a failed attempt there
     * (the third one in a row blocks the card).
     *
     * @return true if the PIN is correct, false if it is wrong or the card has no PIN set
     */
    boolean validatePin(CardId cardId, String pin);

    /**
     * Verifies the CVV against card-data (card-not-present payments)
     *
     * @return true if the CVV is correct, false if it is wrong or the card has no CVV assigned
     */
    boolean validateCvv(CardId cardId, String cvv);

    record CardStatusSnapshot(String status, boolean blocked, boolean expired) {

        public boolean isActive() {
            return "ACTIVE".equals(status);
        }
    }
}
