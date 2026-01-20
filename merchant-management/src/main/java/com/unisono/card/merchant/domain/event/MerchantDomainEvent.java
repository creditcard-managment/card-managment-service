package com.unisono.card.merchant.domain.event;

import java.time.LocalDateTime;

/**
 * Interfaz base para todos los eventos de dominio de Merchant
 */
public interface MerchantDomainEvent {

    /**
     * Identificador único del evento
     */
    String getEventId();

    /**
     * Tipo de evento
     */
    String getEventType();

    /**
     * ID del agregado que generó el evento
     */
    String getAggregateId();

    /**
     * Timestamp de cuándo ocurrió el evento
     */
    LocalDateTime getOccurredOn();

    /**
     * Versión del evento para compatibilidad hacia atrás
     */
    Integer getVersion();
}
