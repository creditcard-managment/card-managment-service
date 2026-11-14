package com.unisono.card.data.domain.port.output;

/**
 * Port de salida: registro de los eventos de Kafka que card-data ya procesó (deduplicación por eventId)
 */
public interface ProcessedEventPort {

    /**
     * Registra el evento en la transacción actual.
     *
     * @return false si ya estaba registrado (el evento se repitió)
     */
    boolean markProcessed(String eventId, String topic);
}
