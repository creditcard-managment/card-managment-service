package com.unisono.card.data.domain.port.input;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.shared.domain.valueobject.CardId;

/**
 * Use Case: Desbloquear una tarjeta
 */
public interface UnblockCardUseCase {

    /**
     * Desbloquea una tarjeta previamente bloqueada
     *
     * @param command datos para desbloquear
     * @return la tarjeta desbloqueada
     */
    Card execute(UnblockCardCommand command);

    /**
     * Command para desbloquear tarjeta
     */
    record UnblockCardCommand(
            CardId cardId,
            String reason,
            String performedBy
    ) {}
}
