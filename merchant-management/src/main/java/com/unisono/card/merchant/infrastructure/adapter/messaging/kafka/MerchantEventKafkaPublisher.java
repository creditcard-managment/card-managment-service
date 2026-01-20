package com.unisono.card.merchant.infrastructure.adapter.messaging.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unisono.card.merchant.application.port.output.MerchantEventPublisherPort;
import com.unisono.card.merchant.domain.event.MerchantDomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación de MerchantEventPublisherPort usando Kafka
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantEventKafkaPublisher implements MerchantEventPublisherPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC_PREFIX = "merchant.events.";

    @Override
    public void publish(MerchantDomainEvent event) {
        try {
            String topic = TOPIC_PREFIX + event.getEventType().toLowerCase();
            String eventJson = objectMapper.writeValueAsString(event);

            log.debug("Publishing event to Kafka topic {}: {}", topic, event.getEventType());

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                    topic,
                    event.getAggregateId(),
                    eventJson
            );

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish event {} to Kafka", event.getEventType(), ex);
                } else {
                    log.info("Successfully published event {} to Kafka topic {}",
                            event.getEventType(), topic);
                }
            });

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event {}", event.getEventType(), e);
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    @Override
    public void publishAll(List<MerchantDomainEvent> events) {
        events.forEach(this::publish);
    }
}
