package com.unisono.card.payment.infrastructure.adapter.output.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unisono.card.events.config.EventSerializationConfig;
import com.unisono.card.events.domain.DomainEvent;
import com.unisono.card.payment.domain.event.PaymentEvent;
import com.unisono.card.payment.domain.port.output.PaymentEventPublisher;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.PaymentOutboxEventEntity;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.repository.JpaPaymentOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Publica los eventos de pagos a través del outbox (tabla payment_outbox_events)
 *
 * Lo que se guarda (y se publica) es el contrato de integración de card-events con campos planos
 * ({@link PaymentIntegrationEventMapper}), serializado con {@link EventSerializationConfig}, igual que los
 * eventos de tarjeta. El evento de dominio solo define el topic y la key.
 *
 * El evento se guarda en la misma transacción que la transacción de pago: si no confirma, el evento
 * no existe. {@link PaymentOutboxRelay} lo publica en Kafka después del commit (at-least-once), con el
 * card id como key: los eventos de una tarjeta quedan en la misma partición y en orden.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPaymentEventPublisher implements PaymentEventPublisher {

    private final JpaPaymentOutboxEventRepository repository;
    private final PaymentEventTopicResolver topicResolver;
    private final PaymentIntegrationEventMapper integrationEvents;
    private final ObjectMapper objectMapper = EventSerializationConfig.objectMapper();

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(DomainEvent event) {
        if (event == null) {
            log.warn("Attempted to publish null event");
            return;
        }

        String topic = topicResolver.resolveTopicForEvent(event);
        DomainEvent contract = integrationEvents.toIntegrationEvent(event);
        PaymentOutboxEventEntity entity = PaymentOutboxEventEntity.builder()
                .eventId(contract.getEventId())
                .aggregateId(keyOf(event))
                .topic(topic)
                .eventType(contract.getEventType())
                .payload(serialize(contract))
                .createdAt(LocalDateTime.now())
                .build();

        repository.save(entity);
        log.debug("Event {} enqueued in outbox for topic '{}' (card {})", event.getEventType(), topic,
                entity.getAggregateId());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishAll(Iterable<DomainEvent> events) {
        if (events == null) {
            log.warn("Attempted to publish null events collection");
            return;
        }
        events.forEach(this::publish);
    }

    private static String keyOf(DomainEvent event) {
        if (event instanceof PaymentEvent paymentEvent && paymentEvent.getCardId() != null) {
            return paymentEvent.getCardId().getValue();
        }
        throw new IllegalArgumentException("Event " + event.getEventType() + " does not belong to a card");
    }

    private String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize event " + event.getEventType(), e);
        }
    }
}
