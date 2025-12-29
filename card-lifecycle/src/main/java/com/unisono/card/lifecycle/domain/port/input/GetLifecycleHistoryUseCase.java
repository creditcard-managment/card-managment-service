package com.unisono.card.lifecycle.domain.port.input;

import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.shared.domain.valueobject.CardId;

import java.util.List;

/**
 * Use Case: Obtener historial de transiciones
 */
public interface GetLifecycleHistoryUseCase {

    /**
     * Obtiene el historial completo de transiciones de una tarjeta
     */
    List<StateTransition> getHistory(CardId cardId);

    /**
     * Obtiene solo las transiciones exitosas
     */
    List<StateTransition> getSuccessfulTransitions(CardId cardId);

    /**
     * Obtiene solo las transiciones fallidas
     */
    List<StateTransition> getFailedTransitions(CardId cardId);
}
