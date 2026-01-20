package com.unisono.card.lifecycle.infrastructure.statemachine.action;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.lifecycle.domain.model.CardLifecycle;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudBlockAction implements Action<CardLifecycleState, CardLifecycleEvent> {

    private final CardPort cardPort;

    @Override
    public void execute(StateContext<CardLifecycleState, CardLifecycleEvent> context) {
        CardId cardId = context.getExtendedState().get("cardId", CardId.class);
        String reason = context.getExtendedState().get("reason", String.class);
        String blockedBy = context.getExtendedState().get("blockedBy", String.class);

        try {
            log.info("Executing fraud block action for card: {} - Reason: {} - By: {}",
                    cardId.getValue(), reason, blockedBy);

            Card card = cardPort.findById(cardId)
                    .orElseThrow(() -> new NotFoundException("Card not found: " + cardId.getValue()));

            // ✅ Usar CardLifecycle
            CardLifecycle lifecycle = new CardLifecycle(card);
            lifecycle.blockForFraud();

            cardPort.save(card);

            log.info("Card blocked for fraud successfully: {}", cardId.getValue());

        } catch (Exception e) {
            log.error("Error blocking card for fraud: {}", cardId.getValue(), e);
            context.getExtendedState().getVariables().put("error", e.getMessage());
            throw new RuntimeException("Failed to block card for fraud: " + cardId.getValue(), e);
        }
    }
}
