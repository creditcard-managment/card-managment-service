package com.unisono.card.data.domain.port.output;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.ClientId;

import java.util.List;
import java.util.Optional;

/**
 * Port de salida: Repositorio de tarjetas
 *
 * Esta interface será implementada por el adaptador de persistencia (JPA).
 */
public interface CardRepository {

    /**
     * Guarda o actualiza una tarjeta
     */
    Card save(Card card);

    /**
     * Busca una tarjeta por su ID
     */
    Optional<Card> findById(CardId cardId);

    /**
     * Busca todas las tarjetas de un cliente
     */
    List<Card> findByClientId(ClientId clientId);

    /**
     * Busca una tarjeta por los últimos 4 dígitos
     * (requiere también el clientId por seguridad)
     */
    Optional<Card> findByLastFourDigitsAndClientId(String lastFourDigits, ClientId clientId);

    /**
     * Busca una tarjeta por el número completo (encriptado en BD)
     */
    Optional<Card> findByCardNumber(String encryptedCardNumber);

    /**
     * Elimina una tarjeta (soft delete recomendado)
     */
    void delete(CardId cardId);

    /**
     * Verifica si existe una tarjeta
     */
    boolean existsById(CardId cardId);
}
