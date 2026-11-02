package com.unisono.card.data.infrastructure.adapter.output.event;

import com.unisono.card.data.domain.event.CardCreatedDomainEvent;
import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.events.domain.CardCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener de eventos de dominio
 * Se ejecuta DESPUÉS del commit de la transacción
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardEventListener {

    private final KafkaEventPublisher kafkaEventPublisher;

    /**
     * Escucha el evento CardCreatedDomainEvent
     * Se ejecuta DESPUÉS de que la transacción haga COMMIT
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCardCreated(CardCreatedDomainEvent event) {
        log.info("=================================================");
        log.info("🟢 [CardEventListener] Transaction committed!");
        log.info("🟢 Handling CardCreatedDomainEvent");
        log.info("🟢 Card ID: {}", event.getCard().getId().getValue());
        log.info("🟢 Client ID: {}", event.getCard().getClientId().getValue());
        log.info("=================================================");

        try {
            // Construir evento de Kafka
            CardCreatedEvent kafkaEvent = new CardCreatedEvent(
                    event.getCard().getId().getValue(),
                    event.getCard().getClientId().getValue(),
                    event.getCard().getHolder().getFullName(),
                    event.getCard().getType().name(),
                    event.getCard().getCategory().name(),
                    "system"
            );

            log.info("🟢 Kafka event created: {}", kafkaEvent.getEventType());
            log.info("🟢 Publishing to Kafka topic: {}", KafkaTopics.CARD_CREATED);

            // Publicar a Kafka
            kafkaEventPublisher.publish(
                    KafkaTopics.CARD_CREATED,
                    event.getCard().getId().getValue(),
                    kafkaEvent
            );

            log.info("🟢 ✅ CardCreatedEvent published to Kafka SUCCESSFULLY!");
            log.info("=================================================");

        } catch (Exception e) {
            log.error("=================================================");
            log.error("🔴 ❌ ERROR publishing CardCreatedEvent to Kafka");
            log.error("🔴 Error: {}", e.getMessage(), e);
            log.error("=================================================");
        }
    }
}
