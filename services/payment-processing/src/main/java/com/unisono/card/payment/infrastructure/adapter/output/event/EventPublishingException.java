package com.unisono.card.payment.infrastructure.adapter.output.event;

/**
 * Exception thrown when event publishing fails
 */
public class EventPublishingException extends RuntimeException {

    public EventPublishingException(String message) {
        super(message);
    }

    public EventPublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}
