package com.unisono.card.lifecycle.infrastructure.statemachine;

import com.unisono.card.lifecycle.domain.exception.InvalidStateTransitionException;
import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.domain.model.TransitionContext;
import com.unisono.card.lifecycle.domain.port.output.LifecycleStateMachinePort;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Ejecuta la state machine para una tarjeta.
 *
 * Por cada evento crea una máquina nueva, la posiciona en el estado real de la tarjeta
 * (leído de card-data) y le envía el evento con los headers que esperan los guards
 * ({@value #HEADER_CARD_ID} y {@value #HEADER_CONTEXT}). La máquina no persiste estado:
 * el dueño del estado sigue siendo card-data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringStateMachineAdapter implements LifecycleStateMachinePort {

    public static final String HEADER_CARD_ID = "cardId";
    public static final String HEADER_CONTEXT = "context";

    /** Variable del extended state donde un guard deja el motivo del rechazo. */
    public static final String GUARD_REJECTION = "guardRejection";

    private final StateMachineFactory<CardLifecycleState, CardLifecycleEvent> factory;

    @Override
    public CardLifecycleState fire(
            CardId cardId,
            CardLifecycleState currentState,
            CardLifecycleEvent event,
            TransitionContext context
    ) {
        StateMachine<CardLifecycleState, CardLifecycleEvent> machine = factory.getStateMachine(cardId.getValue());
        try {
            positionAt(machine, currentState);

            Message<CardLifecycleEvent> message = MessageBuilder.withPayload(event)
                    .setHeader(HEADER_CARD_ID, cardId)
                    .setHeader(HEADER_CONTEXT, context)
                    .build();

            List<StateMachineEventResult<CardLifecycleState, CardLifecycleEvent>> results =
                    machine.sendEvent(Mono.just(message)).collectList().block();

            boolean accepted = results != null && results.stream()
                    .anyMatch(r -> r.getResultType() == StateMachineEventResult.ResultType.ACCEPTED);

            CardLifecycleState reached = machine.getState() != null ? machine.getState().getId() : null;

            if (!accepted || reached == null || reached == currentState) {
                String guardRejection = machine.getExtendedState().get(GUARD_REJECTION, String.class);
                throw guardRejection != null
                        ? new InvalidStateTransitionException(guardRejection)
                        : new InvalidStateTransitionException(currentState, event);
            }

            log.debug("State machine aceptó {} para {}: {} → {}", event, cardId.getValue(), currentState, reached);
            return reached;
        } finally {
            machine.stopReactively().block();
        }
    }

    private static void positionAt(
            StateMachine<CardLifecycleState, CardLifecycleEvent> machine,
            CardLifecycleState state
    ) {
        machine.stopReactively().block();
        machine.getStateMachineAccessor().doWithAllRegions(access ->
                access.resetStateMachineReactively(new DefaultStateMachineContext<>(state, null, null, null)).block());
        machine.startReactively().block();
    }
}
