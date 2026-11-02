package com.unisono.card.lifecycle.infrastructure.adapter.input.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.events.domain.CardCreatedEvent;
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

/**
 * Consumer de eventos de Kafka para card-lifecycle
 *
 * Idempotente: card-data publica con un outbox at-least-once, así que el mismo
 * card.created puede llegar más de una vez. Cada tarjeta tiene una sola transición
 * CREATE; si ya existe, el mensaje se confirma sin registrarla de nuevo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardEventConsumer {

    private static final String CREATE_EVENT = "CREATE";

    private final JpaStateTransitionRepository stateTransitionRepository;
    private final ObjectMapper kafkaObjectMapper; // ✅ Inyectar ObjectMapper

    /**
     * Escucha eventos CardCreatedEvent del topic card.created
     * Recibe el mensaje como String y lo deserializa manualmente
     */
    @KafkaListener(
            topics = KafkaTopics.CARD_CREATED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCardCreatedEvent(
            @Payload String eventJson, // ✅ Recibir como String
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("=================================================");
        log.info("📥 [CardEventConsumer] Received message from Kafka");
        log.info("📥 Partition: {}, Offset: {}", partition, offset);
        log.info("📥 Raw JSON: {}", eventJson);
        log.info("=================================================");

        try {
            // ✅ Deserializar manualmente usando ObjectMapper
            CardCreatedEvent event = kafkaObjectMapper.readValue(eventJson, CardCreatedEvent.class);

            log.info("📥 Deserialized CardCreatedEvent:");
            log.info("📥 Event ID: {}", event.getEventId());
            log.info("📥 Card ID: {}", event.getCardId());
            log.info("📥 Client ID: {}", event.getClientId());
            log.info("📥 Holder: {}", event.getHolderFullName());
            log.info("📥 Type: {}", event.getCardType());
            log.info("📥 Category: {}", event.getCardCategory());

            // Registrar transición de estado CREATE (ignora duplicados)
            registerCreateTransition(event);

            // Confirmar procesamiento
            acknowledgment.acknowledge();

            log.info("✅ CardCreatedEvent processed successfully and acknowledged");
            log.info("=================================================");

        } catch (Exception e) {
            log.error("=================================================");
            log.error("❌ ERROR processing CardCreatedEvent: {}", e.getMessage(), e);
            log.error("=================================================");
            // NO hacer acknowledge - el mensaje se reprocesará
        }
    }

    /**
     * Registra la transición de estado CREATE en state_transitions
     */
    private void registerCreateTransition(CardCreatedEvent event) {
        if (stateTransitionRepository.existsByCardIdAndEvent(event.getCardId(), CREATE_EVENT)) {
            log.info("🔁 Duplicate CardCreatedEvent {} for card {}: CREATE already registered, skipping",
                    event.getEventId(), event.getCardId());
            return;
        }

        log.info("💾 Registering CREATE transition for card: {}", event.getCardId());

        StateTransitionEntity transition = StateTransitionEntity.builder()
                .cardId(event.getCardId())
                .fromState(null)
                .toState("REQUESTED")
                .event(CREATE_EVENT)
                .reason("Solicitud inicial de tarjeta")
                .performedBy(event.getCreatedBy())
                .transitionedAt(event.getOccurredOn())
                .successful(true)
                .errorMessage(null)
                .metadata("{\"eventId\":\"" + event.getEventId() + "\"}")
                .build();

        StateTransitionEntity saved;
        try {
            saved = stateTransitionRepository.saveAndFlush(transition);
        } catch (DataIntegrityViolationException e) {
            // Otro consumo del mismo evento lo registró en paralelo (índice único uq_state_transitions_create)
            log.info("🔁 Duplicate CardCreatedEvent {} for card {}: CREATE registered concurrently, skipping",
                    event.getEventId(), event.getCardId());
            return;
        }

        log.info("💾 ✅ CREATE transition saved with ID: {}", saved.getId());
        log.info("💾 Card ID: {}", saved.getCardId());
        log.info("💾 From: {} → To: {}", saved.getFromState(), saved.getToState());
    }
}
