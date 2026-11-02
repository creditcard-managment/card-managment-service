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
     * @param cardId identificador de la tarjeta
     * @return la tarjeta desbloqueada
     */
    Card execute(CardId cardId);
}
