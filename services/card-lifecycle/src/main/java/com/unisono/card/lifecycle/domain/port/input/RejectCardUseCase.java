package com.unisono.card.lifecycle.domain.port.input;

import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.shared.domain.valueobject.CardId;

/**
 * Use Case: Rechazar solicitud de tarjeta
 */
public interface RejectCardUseCase {

    /**
     * Rechaza una solicitud de tarjeta
     * REQUESTED → REJECTED
     */
    StateTransition execute(RejectCardCommand command);

    record RejectCardCommand(
            CardId cardId,
            String reason,
            String rejectedBy
    ) {}
}
