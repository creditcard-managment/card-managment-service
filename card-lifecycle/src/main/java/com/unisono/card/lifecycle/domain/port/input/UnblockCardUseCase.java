package com.unisono.card.lifecycle.domain.port.input;

import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.shared.domain.valueobject.CardId;

/**
 * Use Case: Desbloquear tarjeta
 */
public interface UnblockCardUseCase {

    /**
     * Desbloquea una tarjeta
     * BLOCKED → ACTIVE
     * BLOCKED_FRAUD → ACTIVE
     */
    StateTransition execute(UnblockCardCommand command);

    record UnblockCardCommand(
            CardId cardId,
            String reason,
            String unblockedBy
    ) {}
}
