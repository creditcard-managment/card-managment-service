package com.unisono.card.payment.domain.event;

import com.unisono.card.shared.domain.valueobject.CardId;

/**
 * Evento de payment-processing. Todos pertenecen a una tarjeta: el card id es la key del mensaje en Kafka,
 * así los eventos de una misma tarjeta (y de cada una de sus transacciones) se consumen en orden.
 */
public interface PaymentEvent {

    CardId getCardId();
}
