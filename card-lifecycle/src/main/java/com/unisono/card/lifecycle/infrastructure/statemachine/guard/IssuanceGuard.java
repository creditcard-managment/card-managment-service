package com.unisono.card.lifecycle.infrastructure.statemachine.guard;


import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.domain.model.TransitionContext;
import com.unisono.card.shared.domain.valueobject.CVV;
import com.unisono.card.shared.domain.valueobject.CardNumber;
import com.unisono.card.shared.domain.valueobject.ExpirationDate;
import com.unisono.card.shared.domain.valueobject.PIN;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

/**
 * Guard para validar emisión de tarjeta
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IssuanceGuard implements Guard<CardLifecycleState, CardLifecycleEvent> {

    @Override
    public boolean evaluate(StateContext<CardLifecycleState, CardLifecycleEvent> context) {
        TransitionContext transitionContext = (TransitionContext)context.getMessageHeader("context");

        if (transitionContext == null) {
            log.warn("No transition context provided for issuance");
            return false;
        }

        // Validar que tenga todos los datos necesarios para emitir
        CardNumber cardNumber = (CardNumber) transitionContext.get("cardNumber");
        CVV cvv = (CVV) transitionContext.get("cvv");
        PIN pin = (PIN) transitionContext.get("pin");
        ExpirationDate expirationDate = (ExpirationDate) transitionContext.get("expirationDate");

        if (cardNumber == null) {
            log.warn("Issuance requires card number");
            return false;
        }

        if (cvv == null) {
            log.warn("Issuance requires CVV");
            return false;
        }

        if (pin == null) {
            log.warn("Issuance requires PIN");
            return false;
        }

        if (expirationDate == null) {
            log.warn("Issuance requires expiration date");
            return false;
        }

        // Validar que la fecha de vencimiento no esté vencida
        if (expirationDate.isExpired()) {
            log.warn("Cannot issue card with expired date");
            return false;
        }

        log.debug("Issuance guard passed");
        return true;
    }
}
