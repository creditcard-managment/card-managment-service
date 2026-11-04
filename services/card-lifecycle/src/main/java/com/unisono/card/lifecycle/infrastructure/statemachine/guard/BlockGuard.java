package com.unisono.card.lifecycle.infrastructure.statemachine.guard;

import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.domain.model.TransitionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

import static com.unisono.card.lifecycle.infrastructure.statemachine.guard.GuardRejection.*;

/**
 * Guard para validar bloqueo de tarjeta
 */
@Slf4j
@Component
public class BlockGuard implements Guard<CardLifecycleState, CardLifecycleEvent> {

    @Override
    public boolean evaluate(StateContext<CardLifecycleState, CardLifecycleEvent> context) {
        TransitionContext transitionContext = transitionContext(context);

        if (transitionContext == null) {
            return reject(context, "No se informó el contexto del bloqueo");
        }
        if (isBlank(transitionContext.getReason())) {
            return reject(context, "El bloqueo requiere una razón");
        }

        log.debug("Block guard passed");
        return true;
    }
}
