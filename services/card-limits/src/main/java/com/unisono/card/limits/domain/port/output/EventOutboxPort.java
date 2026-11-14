package com.unisono.card.limits.domain.port.output;

import com.unisono.card.events.domain.DomainEvent;

/**
 * Port de salida: outbox de eventos de card-limits
 *
 * Encola el evento en la misma transacción que el cambio del límite; el relay lo publica en Kafka
 * después del commit. Si la transacción no confirma, el evento no existe.
 */
public interface EventOutboxPort {

    /**
     * @param topic       topic de Kafka destino
     * @param aggregateId id de la tarjeta (key del mensaje: los eventos de una tarjeta quedan en orden)
     * @param event       evento a publicar
     */
    void enqueue(String topic, String aggregateId, DomainEvent event);
}
