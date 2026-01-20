package com.unisono.card.events.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Clase base para todos los eventos de dominio
 */
@Getter
public abstract class DomainEvent {

    private final String eventId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSSSSS][.SSS]")
    private final LocalDateTime occurredOn;

    private final String eventType;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.eventType = this.getClass().getSimpleName();
    }

    protected DomainEvent(String eventId, LocalDateTime occurredOn) {
        this.eventId = eventId;
        this.occurredOn = occurredOn;
        this.eventType = this.getClass().getSimpleName();
    }
}
