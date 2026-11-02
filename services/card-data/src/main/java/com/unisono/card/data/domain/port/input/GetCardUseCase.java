package com.unisono.card.data.domain.port.input;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.ClientId;

import java.util.List;
import java.util.Optional;

/**
 * Use Case: Obtener información de tarjetas
 */
public interface GetCardUseCase {

    /**
     * Obtiene una tarjeta por su ID
     *
     * @param cardId identificador de la tarjeta
     * @return la tarjeta si existe
     */
    Optional<Card> findById(CardId cardId);

    /**
     * Obtiene todas las tarjetas de un cliente
     *
     * @param clientId identificador del cliente
     * @return lista de tarjetas del cliente
     */
    List<Card> findByClientId(ClientId clientId);

    /**
     * Obtiene una tarjeta por su número enmascarado
     *
     * @param lastFourDigits últimos 4 dígitos
     * @param clientId ID del cliente (para seguridad)
     * @return la tarjeta si existe y pertenece al cliente
     */
    Optional<Card> findByLastFourDigits(String lastFourDigits, ClientId clientId);
}
