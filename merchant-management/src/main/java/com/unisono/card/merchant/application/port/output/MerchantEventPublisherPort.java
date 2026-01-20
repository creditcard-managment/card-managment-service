package com.unisono.card.merchant.application.port.output;

import com.unisono.card.merchant.domain.event.MerchantDomainEvent;

/**
 * Puerto de salida para publicar eventos de dominio
 */
public interface MerchantEventPublisherPort {

    /**
     * Publica un evento de dominio
     */
    void publish(MerchantDomainEvent event);

    /**
     * Publica múltiples eventos de dominio
     */
    void publishAll(java.util.List<MerchantDomainEvent> events);
}
