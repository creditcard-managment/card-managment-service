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

import static com.unisono.card.lifecycle.infrastructure.statemachine.SpringStateMachineAdapter.HEADER_CARD_ID;

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
        CardId cardId = (CardId) context.getMessageHeader(HEADER_CARD_ID);

        if (cardId == null) {
            return GuardRejection.reject(context, "No se informó la tarjeta a activar");
        }

        if (!creditLimitPort.canActivate(cardId)) {
            return GuardRejection.reject(context,
                    "No se puede activar la tarjeta: debe tener límite de crédito asignado. CardId: " + cardId.getValue());
        }

        log.debug("Activation guard passed for card: {}", cardId.getValue());
        return true;
    }
}
