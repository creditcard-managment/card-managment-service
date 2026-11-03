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

    record CardStatusSnapshot(String status, boolean blocked, boolean expired) {

        public boolean isActive() {
            return "ACTIVE".equals(status);
        }
    }
}
