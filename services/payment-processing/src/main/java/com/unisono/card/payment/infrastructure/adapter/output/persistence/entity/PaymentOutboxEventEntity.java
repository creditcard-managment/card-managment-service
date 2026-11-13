package com.unisono.card.payment.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA Entity para payment_outbox_events (V12)
 *
 * Eventos pendientes de publicar en Kafka. Se escriben en la misma transacción
 * que el cambio de la transacción de pago y los publica PaymentOutboxRelay. aggregate_id = card id (key en Kafka).
 *
 * Estados: pendiente (published_at y failed_at nulos), publicado (published_at)
 * o fallido por un error permanente (failed_at; queda para revisión manual).
 */
@Entity
@Table(name = "payment_outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOutboxEventEntity {

    private static final int MAX_ERROR_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "aggregate_id", nullable = false, length = 36)
    private String aggregateId;

    @Column(name = "topic", nullable = false, length = 100)
    private String topic;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    public void markPublished() {
        this.attempts++;
        this.publishedAt = LocalDateTime.now();
        this.lastError = null;
    }

    /**
     * Error transitorio (p. ej. broker caído): el evento sigue pendiente y se reintenta
     */
    public void markFailed(String error) {
        this.attempts++;
        this.lastError = truncate(error);
    }

    /**
     * Error permanente: el evento deja de reintentarse y queda para revisión manual
     */
    public void markDead(String error) {
        this.attempts++;
        this.failedAt = LocalDateTime.now();
        this.lastError = truncate(error);
    }

    private static String truncate(String error) {
        return error != null && error.length() > MAX_ERROR_LENGTH
                ? error.substring(0, MAX_ERROR_LENGTH)
                : error;
    }
}
