package com.unisono.card.data.domain.port.input;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.shared.domain.valueobject.CardId;

/**
 * Use Case: Bloquear una tarjeta
 */
public interface BlockCardUseCase {

    /**
     * Bloquea una tarjeta (temporalmente)
     *
     * @param command datos para bloquear
     * @return la tarjeta bloqueada
     */
    Card execute(BlockCardCommand command);

    /**
     * Bloquea una tarjeta por sospecha de fraude
     *
     * @param command datos para bloquear por fraude
     * @return la tarjeta bloqueada
     */
    Card executeForFraud(BlockCardCommand command);

    /**
     * Command para bloquear tarjeta
     */
    record BlockCardCommand(
            CardId cardId,
            String reason
    ) {}
}
