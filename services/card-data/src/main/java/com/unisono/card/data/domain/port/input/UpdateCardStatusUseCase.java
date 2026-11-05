package com.unisono.card.data.domain.port.input;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import com.unisono.card.shared.domain.valueobject.CardId;

/**
 * Use Case: Actualizar el estado de una tarjeta
 */
public interface UpdateCardStatusUseCase {

    /**
     * Actualiza el estado de una tarjeta
     *
     * @param command datos para actualizar el estado
     * @return la tarjeta actualizada
     */
    Card execute(UpdateStatusCommand command);

    /**
     * Command para actualizar estado
     */
    record UpdateStatusCommand(
            CardId cardId,
            CardStatus newStatus,
            String reason,
            String performedBy
    ) {}
}
