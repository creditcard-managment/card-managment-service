package com.unisono.card.lifecycle.application.service;

import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.lifecycle.domain.model.TransitionContext;
import com.unisono.card.lifecycle.domain.port.input.RejectCardUseCase;
import com.unisono.card.lifecycle.domain.port.output.CardDataClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Rechazo (REQUESTED → REJECTED). Valida con la state machine y aplica el cambio en card-data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RejectCardService implements RejectCardUseCase {

    private final CardDataClient cardDataClient;
    private final LifecycleTransitionExecutor executor;

    @Override
    public StateTransition execute(RejectCardCommand command) {
        log.info("Ejecutando rechazo de tarjeta: {}", command.cardId().getValue());

        StateTransition transition = executor.execute(
                command.cardId(),
                CardLifecycleEvent.REJECT,
                CardLifecycleState.REJECTED,
                TransitionContext.of(command.reason(), command.rejectedBy()),
                () -> cardDataClient.updateStatus(command.cardId(), CardLifecycleState.REJECTED.name())
        );

        log.info("Tarjeta rechazada: {}", command.cardId().getValue());
        return transition;
    }
}
