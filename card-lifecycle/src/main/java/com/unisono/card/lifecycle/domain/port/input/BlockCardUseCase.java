package com.unisono.card.lifecycle.domain.port.input;

import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.shared.domain.valueobject.CardId;

/**
 * Use Case: Bloquear tarjeta
 */
public interface BlockCardUseCase {

    /**
     * Bloquea una tarjeta temporalmente
     * ACTIVE → BLOCKED
     */
    StateTransition executeBlock(BlockCardCommand command);

    /**
     * Bloquea una tarjeta por fraude
     * ACTIVE → BLOCKED_FRAUD
     */
    StateTransition executeBlockFraud(BlockCardCommand command);

    record BlockCardCommand(
            CardId cardId,
            String reason,
            String blockedBy
    ) {}
}
