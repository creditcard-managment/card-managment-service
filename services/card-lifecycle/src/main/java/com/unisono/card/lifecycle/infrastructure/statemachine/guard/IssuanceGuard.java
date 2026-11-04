package com.unisono.card.lifecycle.infrastructure.statemachine.guard;

import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.domain.model.TransitionContext;
import com.unisono.card.shared.domain.valueobject.ExpirationDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

import static com.unisono.card.lifecycle.domain.model.TransitionContext.*;
import static com.unisono.card.lifecycle.infrastructure.statemachine.guard.GuardRejection.*;

/**
 * Guard para validar emisión de tarjeta.
 * Espera en el {@link TransitionContext} los datos de emisión (PAN, CVV, PIN y vencimiento).
 */
@Slf4j
@Component
public class IssuanceGuard implements Guard<CardLifecycleState, CardLifecycleEvent> {

    @Override
    public boolean evaluate(StateContext<CardLifecycleState, CardLifecycleEvent> context) {
        TransitionContext transitionContext = transitionContext(context);

        if (transitionContext == null) {
            return reject(context, "No se informó el contexto de la emisión");
        }
        if (transitionContext.get(CARD_NUMBER) == null) {
            return reject(context, "La emisión requiere el número de tarjeta");
        }
        if (transitionContext.get(CVV) == null) {
            return reject(context, "La emisión requiere el CVV");
        }
        if (transitionContext.get(PIN) == null) {
            return reject(context, "La emisión requiere el PIN");
        }
        if (!(transitionContext.get(EXPIRATION_DATE) instanceof ExpirationDate expirationDate)) {
            return reject(context, "La emisión requiere la fecha de vencimiento");
        }
        if (expirationDate.isExpired()) {
            return reject(context, "No se puede emitir una tarjeta con fecha de vencimiento vencida");
        }

        log.debug("Issuance guard passed");
        return true;
    }
}
