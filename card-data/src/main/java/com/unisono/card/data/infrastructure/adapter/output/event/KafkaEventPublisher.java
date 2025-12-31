package com.unisono.card.data.infrastructure.adapter.output.event;

import com.unisono.card.events.domain.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publicador de eventos de dominio usando Kafka
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publica un evento de dominio en Kafka
     *
     * @param topic nombre del topic
     * @param key clave del mensaje (normalmente el ID de la entidad)
     * @param event evento a publicar
     */
    public void publish(String topic, String key, DomainEvent event) {
        log.info("📤 Publishing event to topic '{}' with key '{}': {}", topic, key, event.getEventType());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("✅ Event published successfully to topic '{}', partition {}, offset {}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("❌ Failed to publish event to topic '{}': {}", topic, ex.getMessage(), ex);
            }
        });
    }

    /**
     * Publica un evento de forma síncrona (espera confirmación)
     */
    public void publishSync(String topic, String key, DomainEvent event) {
        try {
            log.info("📤 Publishing event synchronously to topic '{}' with key '{}': {}",
                    topic, key, event.getEventType());

            SendResult<String, Object> result = kafkaTemplate.send(topic, key, event).get();

            log.info("✅ Event published successfully to topic '{}', partition {}, offset {}",
                    topic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (Exception e) {
            log.error("❌ Failed to publish event to topic '{}': {}", topic, e.getMessage(), e);
            throw new EventPublishingException("Failed to publish event to topic: " + topic, e);
        }
    }
}
