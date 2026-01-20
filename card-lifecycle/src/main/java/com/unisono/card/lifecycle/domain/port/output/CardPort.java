package com.unisono.card.lifecycle.domain.port.output;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.shared.domain.valueobject.CardId;

import java.util.Optional;

/**
 * Puerto de salida para acceder a información de tarjetas
 */
public interface CardPort {

    /**
     * Busca una tarjeta por su ID
     */
    Optional<Card> findById(CardId cardId);

    /**
     * Verifica si existe una tarjeta
     */
    boolean exists(CardId cardId);

    /**
     * Persiste los cambios del modelo de dominio
     * (Solo necesario cuando usamos modelo separado del entity)
     */
    void save(Card card);
}