package com.unisono.card.lifecycle.infrastructure.statemachine.listener;

import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.domain.model.TransitionContext;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

/**
 * Listener que monitorea todos los eventos de la State Machine
 *
 * Propósitos:
 * - Logging detallado de transiciones
 * - Auditoría de cambios de estado
 * - Detección de errores
 * - Métricas y monitoreo
 */
@Slf4j
@Component
public class StateMachineEventListener extends StateMachineListenerAdapter<CardLifecycleState, CardLifecycleEvent> {

    // ========================================
    // STATE MACHINE LIFECYCLE
    // ========================================

    @Override
    public void stateMachineStarted(StateMachine<CardLifecycleState, CardLifecycleEvent> stateMachine) {
        log.debug("State Machine started: {}", stateMachine.getId());
    }

    @Override
    public void stateMachineStopped(StateMachine<CardLifecycleState, CardLifecycleEvent> stateMachine) {
        log.debug("State Machine stopped: {}", stateMachine.getId());
    }

    @Override
    public void stateMachineError(
            StateMachine<CardLifecycleState, CardLifecycleEvent> stateMachine,
            Exception exception
    ) {
        log.error("State Machine error for {}: {}",
                stateMachine.getId(),
                exception.getMessage(),
                exception);
    }

    // ========================================
    // STATE CHANGES
    // ========================================

    @Override
    public void stateChanged(
            State<CardLifecycleState, CardLifecycleEvent> from,
            State<CardLifecycleState, CardLifecycleEvent> to
    ) {
        if (from != null && to != null) {
            log.info("State changed: {} → {}",
                    from.getId().getDescription(),
                    to.getId().getDescription());
        } else if (to != null) {
            log.info("Initial state set: {}", to.getId().getDescription());
        }
    }

    @Override
    public void stateEntered(State<CardLifecycleState, CardLifecycleEvent> state) {
        log.debug("Entered state: {}", state.getId().getDescription());
    }

    @Override
    public void stateExited(State<CardLifecycleState, CardLifecycleEvent> state) {
        log.debug("Exited state: {}", state.getId().getDescription());
    }

    // ========================================
    // EVENT HANDLING
    // ========================================

    @Override
    public void eventNotAccepted(Message<CardLifecycleEvent> event) {
        CardId cardId = (CardId) event.getHeaders().get("cardId");
        TransitionContext context = (TransitionContext) event.getHeaders().get("context");

        log.warn("Event NOT ACCEPTED: {} for card: {} - Reason: {} - By: {}",
                event.getPayload().getDescription(),
                cardId,
                context != null ? context.getReason() : "N/A",
                context != null ? context.getPerformedBy() : "N/A");
    }

    // ========================================
    // TRANSITION HANDLING
    // ========================================

    @Override
    public void transition(Transition<CardLifecycleState, CardLifecycleEvent> transition) {
        if (transition.getSource() != null && transition.getTarget() != null) {
            log.info("Transition: {} → {} (Event: {})",
                    transition.getSource().getId().getDescription(),
                    transition.getTarget().getId().getDescription(),
                    transition.getTrigger() != null ?
                            transition.getTrigger().getEvent().getDescription() : "N/A");
        }
    }

    @Override
    public void transitionStarted(Transition<CardLifecycleState, CardLifecycleEvent> transition) {
        log.debug("Transition started from: {}",
                transition.getSource() != null ?
                        transition.getSource().getId().getDescription() : "N/A");
    }

    @Override
    public void transitionEnded(Transition<CardLifecycleState, CardLifecycleEvent> transition) {
        log.debug("Transition ended at: {}",
                transition.getTarget() != null ?
                        transition.getTarget().getId().getDescription() : "N/A");
    }

    // ========================================
    // GUARD EVALUATION
    // ========================================

    public void stateMachineGuard(
            StateContext<CardLifecycleState, CardLifecycleEvent> context,
            boolean passed
    ) {
        CardId cardId = (CardId) context.getMessageHeader("cardId");

        if (passed) {
            log.debug("Guard PASSED for card: {} - Event: {}",
                    cardId,
                    context.getEvent());
        } else {
            log.warn("Guard REJECTED for card: {} - Event: {}",
                    cardId,
                    context.getEvent());
        }
    }

    // ========================================
    // ACTION EXECUTION
    // ========================================

    public void stateMachineAction(StateContext<CardLifecycleState, CardLifecycleEvent> context) {
        CardId cardId = (CardId) context.getMessageHeader("cardId");

        log.debug("Action executing for card: {} - Event: {}",
                cardId,
                context.getEvent());
    }
}
