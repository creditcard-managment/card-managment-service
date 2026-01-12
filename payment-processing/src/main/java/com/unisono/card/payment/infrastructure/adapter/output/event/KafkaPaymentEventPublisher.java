package com.unisono.card.payment.infrastructure.adapter.output.event;

import com.unisono.card.events.domain.DomainEvent;
import com.unisono.card.payment.domain.port.output.PaymentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka implementation of PaymentEventPublisher
 * Publishes domain events to Kafka topics
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPaymentEventPublisher implements PaymentEventPublisher {

    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final PaymentEventTopicResolver topicResolver;

    @Override
    public void publish(DomainEvent event) {
        if (event == null) {
            log.warn("Attempted to publish null event");
            return;
        }

        String topic = topicResolver.resolveTopicForEvent(event);
        String key = extractEventKey(event);

        log.debug("Publishing event {} to topic {}", event.getEventType(), topic);

        try {
            CompletableFuture<SendResult<String, DomainEvent>> future =
                    kafkaTemplate.send(topic, key, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Event published successfully: {} to topic: {} partition: {}",
                            event.getEventType(),
                            topic,
                            result.getRecordMetadata().partition());
                } else {
                    log.error("Failed to publish event {} to topic {}: {}",
                            event.getEventType(), topic, ex.getMessage(), ex);
                }
            });

        } catch (Exception ex) {
            log.error("Error publishing event {} to Kafka: {}",
                    event.getEventType(), ex.getMessage(), ex);
            throw new EventPublishingException("Failed to publish event: " + event.getEventType(), ex);
        }
    }

    @Override
    public void publishAll(Iterable<DomainEvent> events) {
        if (events == null) {
            log.warn("Attempted to publish null events collection");
            return;
        }

        int count = 0;
        for (DomainEvent event : events) {
            publish(event);
            count++;
        }

        log.debug("Published {} events to Kafka", count);
    }

    /**
     * Extract key from event for partitioning
     * Uses event ID to ensure ordering
     */
    private String extractEventKey(DomainEvent event) {
        return event.getEventId().toString();
    }
}
