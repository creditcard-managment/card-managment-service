package com.unisono.card.lifecycle.infrastructure.adapter.input.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.events.domain.CardCreatedEvent;
import com.unisono.card.lifecycle.infrastructure.adapter.output.persistence.entity.StateTransitionEntity;
import com.unisono.card.lifecycle.infrastructure.adapter.output.persistence.repository.JpaStateTransitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumer de eventos de Kafka para card-lifecycle
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardEventConsumer {

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

            // Registrar transición de estado CREATE
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
        log.info("💾 Registering CREATE transition for card: {}", event.getCardId());

        StateTransitionEntity transition = StateTransitionEntity.builder()
                .cardId(event.getCardId())
                .fromState(null)
                .toState("REQUESTED")
                .event("CREATE")
                .reason("Solicitud inicial de tarjeta")
                .performedBy(event.getCreatedBy())
                .transitionedAt(event.getOccurredOn())
                .successful(true)
                .errorMessage(null)
                .metadata(null)
                .build();

        StateTransitionEntity saved = stateTransitionRepository.save(transition);

        log.info("💾 ✅ CREATE transition saved with ID: {}", saved.getId());
        log.info("💾 Card ID: {}", saved.getCardId());
        log.info("💾 From: {} → To: {}", saved.getFromState(), saved.getToState());
    }
}
