package com.unisono.card.lifecycle.infrastructure.adapter.input.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.events.domain.CardActivatedEvent;
import com.unisono.card.events.domain.CardApprovedEvent;
import com.unisono.card.events.domain.CardBlockedEvent;
import com.unisono.card.events.domain.CardCancelledEvent;
import com.unisono.card.events.domain.CardExpiredEvent;
import com.unisono.card.events.domain.CardIssuedEvent;
import com.unisono.card.events.domain.CardRejectedEvent;
import com.unisono.card.events.domain.CardStatusChange;
import com.unisono.card.events.domain.CardUnblockedEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.infrastructure.adapter.output.persistence.entity.StateTransitionEntity;
import com.unisono.card.lifecycle.infrastructure.adapter.output.persistence.repository.JpaStateTransitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Historial de cambios de estado: consume los eventos card.* de cambio de estado que publica card-data y registra
 * cada uno como una transición exitosa en state_transitions.
 *
 * Es la única fuente de las transiciones exitosas: tanto las que orquesta card-lifecycle (aprobar, emitir…) como
 * las que no pasan por acá (bloqueo automático por PIN, POST /block, el job de vencimiento). Las fallidas las
 * sigue registrando {@code LifecycleTransitionExecutor}, porque no generan evento.
 *
 * Idempotente por eventId (índice único uq_state_transitions_event_id): un mensaje repetido se confirma sin
 * registrarlo de nuevo. Usa su propio grupo para arrancar desde el final del topic la primera vez
 * ({@code card-lifecycle.kafka.history.auto-offset-reset}): los cambios anteriores ya están en el historial.
 * Los errores siguen el mismo camino que card.created: reintentos, dlq.card-events y reproceso automático.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardStatusEventConsumer {

    /** Topics que consume y la clase de su contrato. */
    static final Map<String, Class<? extends CardStatusChange>> CONTRACTS = Map.of(
            KafkaTopics.CARD_APPROVED, CardApprovedEvent.class,
            KafkaTopics.CARD_REJECTED, CardRejectedEvent.class,
            KafkaTopics.CARD_ISSUED, CardIssuedEvent.class,
            KafkaTopics.CARD_ACTIVATED, CardActivatedEvent.class,
            KafkaTopics.CARD_BLOCKED, CardBlockedEvent.class,
            KafkaTopics.CARD_UNBLOCKED, CardUnblockedEvent.class,
            KafkaTopics.CARD_CANCELLED, CardCancelledEvent.class,
            KafkaTopics.CARD_EXPIRED, CardExpiredEvent.class
    );

    public static final Set<String> TOPICS = CONTRACTS.keySet();

    private static final String SYSTEM_ACTOR = "system";

    private final JpaStateTransitionRepository stateTransitionRepository;
    private final ObjectMapper kafkaObjectMapper;

    @KafkaListener(
            id = "card-history",
            topics = {
                    KafkaTopics.CARD_APPROVED, KafkaTopics.CARD_REJECTED, KafkaTopics.CARD_ISSUED,
                    KafkaTopics.CARD_ACTIVATED, KafkaTopics.CARD_BLOCKED, KafkaTopics.CARD_UNBLOCKED,
                    KafkaTopics.CARD_CANCELLED, KafkaTopics.CARD_EXPIRED
            },
            groupId = "${spring.kafka.consumer.group-id}-history",
            containerFactory = "kafkaListenerContainerFactory",
            properties = "auto.offset.reset=${card-lifecycle.kafka.history.auto-offset-reset:latest}"
    )
    public void consume(
            @Payload String eventJson,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment
    ) throws JsonProcessingException {
        Class<? extends CardStatusChange> contract = CONTRACTS.get(topic);
        if (contract == null) {
            // No debería pasar: el listener solo escucha los topics de CONTRACTS
            throw new IllegalArgumentException("Unexpected topic " + topic);
        }

        CardStatusChange change = kafkaObjectMapper.readValue(eventJson, contract);
        register(change, lifecycleEventOf(topic, change));
        acknowledgment.acknowledge();
    }

    static CardLifecycleEvent lifecycleEventOf(String topic, CardStatusChange change) {
        return switch (topic) {
            case KafkaTopics.CARD_APPROVED -> CardLifecycleEvent.APPROVE;
            case KafkaTopics.CARD_REJECTED -> CardLifecycleEvent.REJECT;
            case KafkaTopics.CARD_ISSUED -> CardLifecycleEvent.ISSUE;
            case KafkaTopics.CARD_ACTIVATED -> CardLifecycleEvent.ACTIVATE;
            case KafkaTopics.CARD_BLOCKED -> "BLOCKED_FRAUD".equals(change.newStatus())
                    ? CardLifecycleEvent.BLOCK_FRAUD : CardLifecycleEvent.BLOCK;
            case KafkaTopics.CARD_UNBLOCKED -> CardLifecycleEvent.UNBLOCK;
            case KafkaTopics.CARD_CANCELLED -> CardLifecycleEvent.CANCEL;
            case KafkaTopics.CARD_EXPIRED -> CardLifecycleEvent.EXPIRE;
            default -> throw new IllegalArgumentException("Unexpected topic " + topic);
        };
    }

    private void register(CardStatusChange change, CardLifecycleEvent event) {
        if (stateTransitionRepository.existsByEventId(change.getEventId())) {
            log.info("🔁 Duplicate event {} ({}) for card {}: already in the history, skipping",
                    change.getEventId(), event, change.getCardId());
            return;
        }

        StateTransitionEntity transition = StateTransitionEntity.builder()
                .cardId(change.getCardId())
                .fromState(change.getPreviousStatus())
                .toState(change.newStatus())
                .event(event.name())
                .reason(change.getReason())
                .performedBy(change.actor() == null || change.actor().isBlank() ? SYSTEM_ACTOR : change.actor())
                .transitionedAt(change.changedAt())
                .successful(true)
                .metadata("{\"eventId\":\"" + change.getEventId() + "\"}")
                .eventId(change.getEventId())
                .build();

        try {
            stateTransitionRepository.saveAndFlush(transition);
        } catch (DataIntegrityViolationException e) {
            // Otra restricción (FK a cards, NOT NULL): no es un duplicado, el mensaje no se puede procesar
            if (!stateTransitionRepository.existsByEventId(change.getEventId())) {
                throw e;
            }
            log.info("🔁 Duplicate event {} ({}) for card {}: registered concurrently, skipping",
                    change.getEventId(), event, change.getCardId());
            return;
        }

        log.info("💾 {} registered for card {}: {} → {} (event {})", event, change.getCardId(),
                change.getPreviousStatus(), change.newStatus(), change.getEventId());
    }
}
