package com.unisono.card.lifecycle.application.service;

import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.lifecycle.domain.model.TransitionContext;
import com.unisono.card.lifecycle.domain.port.output.CardDataClient;
import com.unisono.card.lifecycle.domain.port.output.LifecycleStateMachinePort;
import com.unisono.card.lifecycle.domain.port.output.StateTransitionRepository;
import com.unisono.card.shared.domain.exception.DomainException;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Plantilla común de las transiciones que se aplican vía card-data
 * (aprobar, emitir, activar):
 *
 * <ol>
 *   <li>lee el estado real de la tarjeta en card-data;</li>
 *   <li>dispara el evento en la state machine (transición + guards);</li>
 *   <li>ejecuta los efectos en card-data;</li>
 *   <li>registra la transición exitosa con el estado origen real.</li>
 * </ol>
 *
 * Si algo falla con una excepción de dominio, registra la transición fallida y la relanza.
 * No es transaccional a propósito: cada guardado de {@code state_transitions} se confirma
 * por separado, así el registro del fallo no se revierte junto con la excepción.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LifecycleTransitionExecutor {

    private final CardDataClient cardDataClient;
    private final LifecycleStateMachinePort stateMachine;
    private final StateTransitionRepository transitionRepository;

    /**
     * Efectos a aplicar en card-data una vez que la state machine aceptó el evento.
     */
    @FunctionalInterface
    public interface TransitionEffects {
        void apply();
    }

    public StateTransition execute(
            CardId cardId,
            CardLifecycleEvent event,
            CardLifecycleState intendedTarget,
            TransitionContext context,
            TransitionEffects effects
    ) {
        CardLifecycleState currentState = null;
        try {
            currentState = CardLifecycleState.valueOf(cardDataClient.getStatus(cardId).name());

            CardLifecycleState target = stateMachine.fire(cardId, currentState, event, context);

            effects.apply();

            return transitionRepository.save(StateTransition.createSuccessful(
                    cardId, currentState.name(), target.name(), event.name(),
                    context.getReason(), context.getPerformedBy()));

        } catch (NotFoundException e) {
            // La tarjeta no existe: no hay a quién asociar la transición fallida (FK a cards).
            throw e;
        } catch (DomainException e) {
            recordFailure(cardId, currentState, intendedTarget, event, context, e);
            throw e;
        }
    }

    private void recordFailure(
            CardId cardId,
            CardLifecycleState fromState,
            CardLifecycleState intendedTarget,
            CardLifecycleEvent event,
            TransitionContext context,
            DomainException cause
    ) {
        log.warn("Transición {} fallida para {}: {}", event, cardId.getValue(), cause.getMessage());
        try {
            transitionRepository.save(StateTransition.createFailed(
                    cardId,
                    fromState != null ? fromState.name() : null,
                    intendedTarget.name(),
                    event.name(),
                    context.getReason(),
                    cause.getMessage(),
                    context.getPerformedBy()));
        } catch (RuntimeException e) {
            // No ocultar el error original por un fallo al auditar.
            log.error("No se pudo registrar la transición fallida {} de {}", event, cardId.getValue(), e);
        }
    }
}
