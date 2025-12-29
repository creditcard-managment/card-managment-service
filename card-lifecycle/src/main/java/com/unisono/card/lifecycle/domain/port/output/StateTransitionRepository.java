package com.unisono.card.lifecycle.domain.port.output;

import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.shared.domain.valueobject.CardId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para gestionar el historial de transiciones de estado
 */
public interface StateTransitionRepository {

    /**
     * Guarda una transición de estado
     */
    StateTransition save(StateTransition transition);

    /**
     * Busca una transición por ID
     */
    Optional<StateTransition> findById(Long id);

    /**
     * Obtiene todas las transiciones de una tarjeta ordenadas por fecha (DESC)
     */
    List<StateTransition> findByCardId(CardId cardId);

    /**
     * Obtiene solo transiciones exitosas de una tarjeta
     */
    List<StateTransition> findSuccessfulByCardId(CardId cardId);

    /**
     * Obtiene solo transiciones fallidas de una tarjeta
     */
    List<StateTransition> findFailedByCardId(CardId cardId);

    /**
     * Obtiene la última transición de una tarjeta
     */
    Optional<StateTransition> findLastTransition(CardId cardId);

    /**
     * Cuenta el número de transiciones de una tarjeta
     */
    long countByCardId(CardId cardId);

    /**
     * Verifica si existen transiciones para una tarjeta
     */
    boolean existsByCardId(CardId cardId);
}
