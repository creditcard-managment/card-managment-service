package com.unisono.card.lifecycle.infrastructure.statemachine.guard;

import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.domain.model.TransitionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;

import static com.unisono.card.lifecycle.infrastructure.statemachine.SpringStateMachineAdapter.GUARD_REJECTION;
import static com.unisono.card.lifecycle.infrastructure.statemachine.SpringStateMachineAdapter.HEADER_CONTEXT;

/**
 * Helpers compartidos por los guards.
 */
@Slf4j
final class GuardRejection {

    private GuardRejection() {
    }

    /**
     * Deja el motivo en el extended state (lo lee SpringStateMachineAdapter) y devuelve {@code false}.
     */
    static boolean reject(StateContext<CardLifecycleState, CardLifecycleEvent> context, String reason) {
        log.warn("Guard rechazó {}: {}", context.getEvent(), reason);
        context.getExtendedState().getVariables().put(GUARD_REJECTION, reason);
        return false;
    }

    static TransitionContext transitionContext(StateContext<CardLifecycleState, CardLifecycleEvent> context) {
        return (TransitionContext) context.getMessageHeader(HEADER_CONTEXT);
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
