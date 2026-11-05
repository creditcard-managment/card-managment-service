package com.unisono.card.lifecycle.application.service;

import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.lifecycle.domain.model.TransitionContext;
import com.unisono.card.lifecycle.domain.port.input.ApproveCardUseCase;
import com.unisono.card.lifecycle.domain.port.output.CardDataClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Aprobación (REQUESTED → APPROVED). Valida con la state machine y aplica el cambio en card-data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApproveCardService implements ApproveCardUseCase {

    private final CardDataClient cardDataClient;
    private final LifecycleTransitionExecutor executor;

    @Override
    public StateTransition execute(ApproveCardCommand command) {
        log.info("Ejecutando aprobación de tarjeta: {}", command.cardId().getValue());

        StateTransition transition = executor.execute(
                command.cardId(),
                CardLifecycleEvent.APPROVE,
                CardLifecycleState.APPROVED,
                TransitionContext.of(command.reason(), command.approvedBy()),
                () -> cardDataClient.updateStatus(command.cardId(), CardLifecycleState.APPROVED.name(),
                        command.reason(), command.approvedBy())
        );

        log.info("Tarjeta aprobada exitosamente: {}", command.cardId().getValue());
        return transition;
    }
}
