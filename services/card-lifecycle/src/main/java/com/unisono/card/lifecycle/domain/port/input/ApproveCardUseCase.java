package com.unisono.card.lifecycle.domain.port.input;

import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.shared.domain.valueobject.CardId;

/**
 * Use Case: Aprobar solicitud de tarjeta
 */
public interface ApproveCardUseCase {

    /**
     * Aprueba una solicitud de tarjeta
     * REQUESTED → APPROVED
     */
    StateTransition execute(ApproveCardCommand command);

    record ApproveCardCommand(
            CardId cardId,
            String reason,
            String approvedBy
    ) {}
}
