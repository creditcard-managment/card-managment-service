package com.unisono.card.data.infrastructure.adapter.output.event;

/**
 * Excepción lanzada cuando falla la publicación de un evento
 */
public class EventPublishingException extends RuntimeException {

    public EventPublishingException(String message) {
        super(message);
    }

    public EventPublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}
