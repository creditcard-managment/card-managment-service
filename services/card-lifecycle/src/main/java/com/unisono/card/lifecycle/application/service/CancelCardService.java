package com.unisono.card.lifecycle.application.service;

import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.lifecycle.domain.model.TransitionContext;
import com.unisono.card.lifecycle.domain.port.input.CancelCardUseCase;
import com.unisono.card.lifecycle.domain.port.output.CardDataClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Cancelación (APPROVED / ISSUED / ACTIVE / BLOCKED / BLOCKED_FRAUD → CANCELLED).
 * Valida con la state machine y cancela vía card-data, que registra cancelled_reason y cancelled_at.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CancelCardService implements CancelCardUseCase {

    private final CardDataClient cardDataClient;
    private final LifecycleTransitionExecutor executor;

    @Override
    public StateTransition execute(CancelCardCommand command) {
        log.info("Ejecutando cancelación de tarjeta: {}", command.cardId().getValue());

        StateTransition transition = executor.execute(
                command.cardId(),
                CardLifecycleEvent.CANCEL,
                CardLifecycleState.CANCELLED,
                TransitionContext.of(command.reason(), command.cancelledBy()),
                () -> cardDataClient.cancel(command.cardId(), command.reason(), command.cancelledBy())
        );

        log.info("Tarjeta cancelada: {}", command.cardId().getValue());
        return transition;
    }
}
