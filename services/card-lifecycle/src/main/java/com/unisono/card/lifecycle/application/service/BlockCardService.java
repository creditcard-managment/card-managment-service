package com.unisono.card.lifecycle.application.service;

import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.lifecycle.domain.model.TransitionContext;
import com.unisono.card.lifecycle.domain.port.input.BlockCardUseCase;
import com.unisono.card.lifecycle.domain.port.output.CardDataClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Bloqueo (ACTIVE → BLOCKED / BLOCKED_FRAUD). Valida con la state machine (BlockGuard) y
 * bloquea vía los endpoints de card-data, que registran block_reason y blocked_at.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlockCardService implements BlockCardUseCase {

    private final CardDataClient cardDataClient;
    private final LifecycleTransitionExecutor executor;

    @Override
    public StateTransition executeBlock(BlockCardCommand command) {
        log.info("Ejecutando bloqueo de tarjeta: {}", command.cardId().getValue());

        return executor.execute(
                command.cardId(),
                CardLifecycleEvent.BLOCK,
                CardLifecycleState.BLOCKED,
                TransitionContext.of(command.reason(), command.blockedBy()),
                () -> cardDataClient.block(command.cardId(), command.reason(), command.blockedBy())
        );
    }

    @Override
    public StateTransition executeBlockFraud(BlockCardCommand command) {
        log.warn("Ejecutando bloqueo por fraude de tarjeta: {}", command.cardId().getValue());

        return executor.execute(
                command.cardId(),
                CardLifecycleEvent.BLOCK_FRAUD,
                CardLifecycleState.BLOCKED_FRAUD,
                TransitionContext.of(command.reason(), command.blockedBy()),
                () -> cardDataClient.blockForFraud(command.cardId(), command.reason(), command.blockedBy())
        );
    }
}
