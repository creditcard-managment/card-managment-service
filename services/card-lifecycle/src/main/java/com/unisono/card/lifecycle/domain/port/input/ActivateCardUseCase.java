package com.unisono.card.lifecycle.domain.port.input;

import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.shared.domain.valueobject.CardId;

/**
 * Use Case: Activar tarjeta
 */
public interface ActivateCardUseCase {

    /**
     * Activa una tarjeta emitida
     * ISSUED → ACTIVE
     */
    StateTransition execute(ActivateCardCommand command);

    record ActivateCardCommand(
            CardId cardId,
            String reason,
            String activatedBy
    ) {}
}
