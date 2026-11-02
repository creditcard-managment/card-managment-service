package com.unisono.card.lifecycle.infrastructure.statemachine.guard;

import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.domain.port.output.CreditLimitPort;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

/**
 * Guard para validar activación de tarjeta
 *
 * Valida que la tarjeta tenga límite de crédito asignado
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivationGuard implements Guard<CardLifecycleState, CardLifecycleEvent> {

    private final CreditLimitPort creditLimitPort;

    @Override
    public boolean evaluate(StateContext<CardLifecycleState, CardLifecycleEvent> context) {
        CardId cardId = (CardId)context.getMessageHeader("cardId");

        if (cardId == null) {
            log.warn("No cardId provided for activation");
            return false;
        }

        // Validar que tenga límite de crédito asignado
        boolean hasLimit = creditLimitPort.hasLimitAssigned(cardId);

        if (!hasLimit) {
            log.warn("Cannot activate card without credit limit: {}", cardId);
            return false;
        }

        log.debug("Activation guard passed for card: {}", cardId);
        return true;
    }
}
