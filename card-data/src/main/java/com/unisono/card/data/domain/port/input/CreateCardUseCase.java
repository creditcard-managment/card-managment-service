package com.unisono.card.data.domain.port.input;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.model.CardCategory;
import com.unisono.card.data.domain.model.CardHolder;
import com.unisono.card.data.domain.model.CardType;
import com.unisono.card.data.infrastructure.adapter.input.rest.dto.UpdateHolderDataRequest;
import com.unisono.card.shared.domain.valueobject.ClientId;

/**
 * Use Case: Crear una nueva tarjeta
 *
 * Crea una tarjeta en estado REQUESTED (solicitud inicial).
 * Este es el primer paso en el ciclo de vida de una tarjeta.
 */
public interface CreateCardUseCase {

    /**
     * Crea una nueva tarjeta de crédito
     *
     * @param command datos necesarios para crear la tarjeta
     * @return la tarjeta creada
     */
    Card execute(CreateCardCommand command);

    void updateHolderData(String cardId, UpdateHolderDataRequest request);

    /**
     * Command para crear una tarjeta
     */
    record CreateCardCommand(
            ClientId clientId,
            CardHolder holder,
            CardType type,
            CardCategory category
    ) {}
}
