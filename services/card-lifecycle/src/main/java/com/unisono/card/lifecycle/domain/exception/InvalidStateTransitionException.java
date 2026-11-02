package com.unisono.card.lifecycle.domain.exception;

import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.shared.domain.exception.DomainException;

/**
 * Excepción lanzada cuando se intenta una transición de estado inválida
 */
public class InvalidStateTransitionException extends DomainException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }

    public InvalidStateTransitionException(
            CardLifecycleState currentState,
            CardLifecycleEvent event
    ) {
        super(String.format(
                "Transición inválida: no se puede ejecutar evento '%s' desde estado '%s'",
                event.getDescription(),
                currentState.getDescription()
        ));
    }

    public InvalidStateTransitionException(
            CardLifecycleState fromState,
            CardLifecycleState toState,
            String reason
    ) {
        super(String.format(
                "Transición inválida de '%s' a '%s': %s",
                fromState.getDescription(),
                toState.getDescription(),
                reason
        ));
    }
}
