package com.unisono.card.lifecycle.infrastructure.statemachine.action;

import com.unisono.card.lifecycle.domain.model.Card;
import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.domain.port.output.CardPort;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

/**
 * Action ejecutada cuando se aprueba una tarjeta
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalAction implements Action<CardLifecycleState, CardLifecycleEvent> {

    private final CardPort cardPort;

    @Override
    public void execute(StateContext<CardLifecycleState, CardLifecycleEvent> context) {
        CardId cardId = context.getExtendedState().get("cardId", CardId.class);
        String reason = context.getExtendedState().get("reason", String.class);
        String approvedBy = context.getExtendedState().get("approvedBy", String.class);

        try {
            log.info("Executing approval action for card: {} - Reason: {} - By: {}",
                    cardId.getValue(), reason, approvedBy);

            // ✅ 1. Buscar tarjeta
            Card card = cardPort.findById(cardId)
                    .orElseThrow(() -> new NotFoundException(
                            "Card not found: " + cardId.getValue()
                    ));

            // ✅ 2. Ejecutar lógica de negocio
            card.approve();

            // ✅ 3. Persistir cambios
            cardPort.save(card);

            log.info("Card approved successfully: {}", cardId.getValue());

        } catch (Exception e) {
            log.error("Error approving card: {}", cardId.getValue(), e);

            // Guardar error en el contexto para que pueda ser registrado
            context.getExtendedState().getVariables().put("error", e.getMessage());

            throw new RuntimeException("Failed to approve card: " + cardId.getValue(), e);
        }
    }
}
