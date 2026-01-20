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
            @Payload CardCreatedEvent event,  // ✅ Directamente el objeto
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("📥 Received CardCreatedEvent:");
        log.info("📥 Card ID: {}", event.getCardId());
        log.info("📥 Client ID: {}", event.getClientId());
        log.info("📥 Partition: {}, Offset: {}", partition, offset);

        try {
            registerCreateTransition(event);
            acknowledgment.acknowledge();
            log.info("✅ Event processed successfully");
        } catch (Exception e) {
            log.error("❌ Error processing event: {}", e.getMessage(), e);
            // No acknowledge - se reprocesará
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
