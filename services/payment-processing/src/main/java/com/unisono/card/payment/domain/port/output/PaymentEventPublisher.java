package com.unisono.card.payment.domain.port.output;

import com.unisono.card.events.domain.DomainEvent;

/**
 * Port for publishing domain events to message broker (Kafka)
 */
public interface PaymentEventPublisher {

    /**
     * Publish a domain event
     */
    void publish(DomainEvent event);

    /**
     * Publish multiple domain events
     */
    void publishAll(Iterable<DomainEvent> events);
}
