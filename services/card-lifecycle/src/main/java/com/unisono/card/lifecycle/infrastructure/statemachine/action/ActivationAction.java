package com.unisono.card.lifecycle.infrastructure.statemachine.action;

import com.unisono.card.lifecycle.domain.model.Card;
import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.domain.port.output.CardPort;
import com.unisono.card.lifecycle.domain.port.output.CreditLimitPort;
import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

/**
 * Action ejecutada cuando se activa una tarjeta
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivationAction implements Action<CardLifecycleState, CardLifecycleEvent> {

    private final CardPort cardPort;
    private final CreditLimitPort creditLimitPort;

    @Override
    public void execute(StateContext<CardLifecycleState, CardLifecycleEvent> context) {
        CardId cardId = context.getExtendedState().get("cardId", CardId.class);
        String reason = context.getExtendedState().get("reason", String.class);
        String activatedBy = context.getExtendedState().get("activatedBy", String.class);

        try {
            log.info("Executing activation action for card: {} - Reason: {} - By: {}",
                    cardId.getValue(), reason, activatedBy);

            // ✅ Validar que tenga límite de crédito asignado
            if (!creditLimitPort.canActivate(cardId)) {
                throw new BusinessRuleException(
                        "Cannot activate card: Credit limit not assigned - " + cardId.getValue()
                );
            }

            // ✅ 1. Buscar tarjeta
            Card card = cardPort.findById(cardId)
                    .orElseThrow(() -> new NotFoundException(
                            "Card not found: " + cardId.getValue()
                    ));

            // ✅ 2. Ejecutar lógica de negocio
            card.activate();

            // ✅ 3. Persistir cambios
            cardPort.save(card);

            log.info("Card activated successfully: {}", cardId.getValue());

        } catch (Exception e) {
            log.error("Error activating card: {}", cardId.getValue(), e);
            context.getExtendedState().getVariables().put("error", e.getMessage());
            throw new RuntimeException("Failed to activate card: " + cardId.getValue(), e);
        }
    }
}
