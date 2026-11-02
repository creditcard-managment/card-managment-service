package com.unisono.card.lifecycle.infrastructure.statemachine.action;

import com.unisono.card.lifecycle.domain.model.Card;
import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.domain.port.output.CardPort;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.valueobject.CVV;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.CardNumber;
import com.unisono.card.shared.domain.valueobject.ExpirationDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

/**
 * Action ejecutada después de emitir una tarjeta
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IssuanceAction implements Action<CardLifecycleState, CardLifecycleEvent> {

    private final CardPort cardPort;

    @Override
    public void execute(StateContext<CardLifecycleState, CardLifecycleEvent> context) {
        CardId cardId = context.getExtendedState().get("cardId", CardId.class);
        CardNumber cardNumber = context.getExtendedState().get("cardNumber", CardNumber.class);
        CVV cvv = context.getExtendedState().get("cvv", CVV.class);
        String pinHash = context.getExtendedState().get("pinHash", String.class);
        ExpirationDate expirationDate = context.getExtendedState().get("expirationDate", ExpirationDate.class);
        String reason = context.getExtendedState().get("reason", String.class);
        String issuedBy = context.getExtendedState().get("issuedBy", String.class);

        try {
            log.info("Executing issuance action for card: {} - Reason: {} - By: {}",
                    cardId.getValue(), reason, issuedBy);

            Card card = cardPort.findById(cardId)
                    .orElseThrow(() -> new NotFoundException("Card not found: " + cardId.getValue()));

            card.issue(cardNumber, cvv, pinHash, expirationDate);
            cardPort.save(card);

            log.info("Card issued successfully: {} - Last 4 digits: {}",
                    cardId.getValue(), card.getLastFourDigits());

        } catch (Exception e) {
            log.error("Error issuing card: {}", cardId.getValue(), e);
            context.getExtendedState().getVariables().put("error", e.getMessage());
            throw new RuntimeException("Failed to issue card: " + cardId.getValue(), e);
        }
    }
}
