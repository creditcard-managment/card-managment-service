package com.unisono.card.lifecycle.infrastructure.statemachine.guard;

import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.domain.model.TransitionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

/**
 * Guard para validar aprobación de tarjeta
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalGuard implements Guard<CardLifecycleState, CardLifecycleEvent> {

    @Override
    public boolean evaluate(StateContext<CardLifecycleState, CardLifecycleEvent> context) {
        TransitionContext transitionContext = (TransitionContext)context.getMessageHeader("context");

        if (transitionContext == null) {
            log.warn("No transition context provided for approval");
            return false;
        }

        // Validar que haya una razón
        if (transitionContext.getReason() == null || transitionContext.getReason().isBlank()) {
            log.warn("Approval requires a reason");
            return false;
        }

        // Validar que haya un aprobador
        if (transitionContext.getPerformedBy() == null || transitionContext.getPerformedBy().isBlank()) {
            log.warn("Approval requires approver information");
            return false;
        }

        log.debug("Approval guard passed");
        return true;
    }
}
