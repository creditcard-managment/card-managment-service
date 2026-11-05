package com.unisono.card.lifecycle.application.service;

import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.lifecycle.domain.model.TransitionContext;
import com.unisono.card.lifecycle.domain.port.input.IssueCardUseCase;
import com.unisono.card.lifecycle.domain.port.output.CardDataClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.unisono.card.lifecycle.domain.model.TransitionContext.*;

/**
 * Emisión (APPROVED → ISSUED). Valida con la state machine (IssuanceGuard) y luego carga
 * los datos de emisión y el nuevo estado en card-data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IssueCardService implements IssueCardUseCase {

    private final CardDataClient cardDataClient;
    private final LifecycleTransitionExecutor executor;

    @Override
    public StateTransition execute(IssueCardCommand command) {
        log.info("Ejecutando emisión de tarjeta: {}", command.cardId().getValue());

        TransitionContext context = TransitionContext.of(command.reason(), command.issuedBy(), Map.of(
                CARD_NUMBER, command.cardNumber(),
                CVV, command.cvv(),
                PIN, command.pin(),
                EXPIRATION_DATE, command.expirationDate()
        ));

        StateTransition transition = executor.execute(
                command.cardId(),
                CardLifecycleEvent.ISSUE,
                CardLifecycleState.ISSUED,
                context,
                () -> {
                    cardDataClient.updateCardData(
                            command.cardId(),
                            command.cardNumber(),
                            command.cvv(),
                            command.pin(),
                            command.expirationDate()
                    );
                    cardDataClient.updateStatus(command.cardId(), CardLifecycleState.ISSUED.name(),
                            command.reason(), command.issuedBy());
                }
        );

        log.info("Tarjeta emitida exitosamente: {}", command.cardId().getValue());
        return transition;
    }
}
