package com.unisono.card.lifecycle.application.service;

import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.lifecycle.domain.model.TransitionContext;
import com.unisono.card.lifecycle.domain.port.input.ActivateCardUseCase;
import com.unisono.card.lifecycle.domain.port.output.CardDataClient;
import com.unisono.card.shared.domain.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Activación (ISSUED → ACTIVE).
 *
 * La state machine valida el estado y el límite de crédito (ActivationGuard). Recién
 * entonces se verifica el PIN contra card-data, para no consumir intentos de PIN en
 * una activación que igual sería rechazada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivateCardService implements ActivateCardUseCase {

    private final CardDataClient cardDataClient;
    private final LifecycleTransitionExecutor executor;

    @Override
    public StateTransition execute(ActivateCardCommand command) {
        log.info("Ejecutando activación de tarjeta: {}", command.cardId().getValue());

        StateTransition transition = executor.execute(
                command.cardId(),
                CardLifecycleEvent.ACTIVATE,
                CardLifecycleState.ACTIVE,
                TransitionContext.of(command.reason(), command.activatedBy()),
                () -> {
                    if (!cardDataClient.validatePin(command.cardId(), command.pin())) {
                        throw new BusinessRuleException(
                                "PIN incorrecto: no se puede activar la tarjeta " + command.cardId().getValue());
                    }
                    cardDataClient.updateStatus(command.cardId(), CardLifecycleState.ACTIVE.name());
                    cardDataClient.updateActivatedAt(command.cardId(), LocalDateTime.now());
                }
        );

        log.info("Tarjeta activada exitosamente: {}", command.cardId().getValue());
        return transition;
    }
}
