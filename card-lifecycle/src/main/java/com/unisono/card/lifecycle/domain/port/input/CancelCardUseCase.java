package com.unisono.card.lifecycle.domain.port.input;

import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.shared.domain.valueobject.CardId;

/**
 * Use Case: Cancelar tarjeta
 */
public interface CancelCardUseCase {

    /**
     * Cancela una tarjeta permanentemente
     * * → CANCELLED
     */
    StateTransition execute(CancelCardCommand command);

    record CancelCardCommand(
            CardId cardId,
            String reason,
            String cancelledBy
    ) {}
}
