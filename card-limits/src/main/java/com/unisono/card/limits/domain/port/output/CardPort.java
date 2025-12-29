package com.unisono.card.limits.domain.port.output;

import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import java.util.Optional;

/**
 * Puerto de salida para acceder a información de tarjetas
 */
public interface CardPort {

    /**
     * Busca una tarjeta por su ID
     */
    Optional<CardInfo> findById(CardId cardId);

    /**
     * Verifica si existe una tarjeta
     */
    boolean exists(CardId cardId);

    /**
     * DTO simple con info mínima de la tarjeta
     */
    record CardInfo(
            CardId id,
            CardStatus status,
            boolean deleted
    ) {
        public boolean isActive() {
            return status == CardStatus.ACTIVE && !deleted;
        }

        public boolean canAssignLimit() {
            return (status == CardStatus.APPROVED || status == CardStatus.ISSUED)
                    && !deleted;
        }
    }
}
