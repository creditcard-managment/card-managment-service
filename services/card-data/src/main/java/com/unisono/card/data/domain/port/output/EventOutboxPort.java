package com.unisono.card.data.domain.port.output;

import com.unisono.card.events.domain.DomainEvent;

/**
 * Port de salida: Outbox de eventos de integración
 *
 * Registra el evento en la misma transacción que el cambio de estado del aggregate.
 * Un relay lo publica en Kafka después del commit (at-least-once).
 */
@FunctionalInterface
public interface EventOutboxPort {

    /**
     * Encola un evento para publicar
     *
     * @param topic       topic de Kafka destino
     * @param aggregateId id del aggregate (se usa como key del mensaje)
     * @param event       evento a publicar
     */
    void enqueue(String topic, String aggregateId, DomainEvent event);
}
