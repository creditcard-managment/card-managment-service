package com.unisono.card.lifecycle.domain.port.output;

import com.unisono.card.lifecycle.domain.exception.InvalidStateTransitionException;
import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.domain.model.TransitionContext;
import com.unisono.card.shared.domain.valueobject.CardId;

/**
 * Puerto de salida que decide si un evento es aplicable a una tarjeta.
 * La implementación ejecuta la state machine (transiciones + guards).
 */
public interface LifecycleStateMachinePort {

    /**
     * Dispara {@code event} sobre una máquina posicionada en {@code currentState}.
     *
     * @return estado destino si la transición fue aceptada
     * @throws InvalidStateTransitionException si no hay transición para el evento
     *         desde el estado actual o algún guard la rechaza
     */
    CardLifecycleState fire(
            CardId cardId,
            CardLifecycleState currentState,
            CardLifecycleEvent event,
            TransitionContext context
    );
}
