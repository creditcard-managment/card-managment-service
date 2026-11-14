package com.unisono.card.payment.infrastructure.adapter.output.event;

import com.unisono.card.events.domain.DomainEvent;
import com.unisono.card.events.domain.payment.PaymentAuthorizedEvent;
import com.unisono.card.events.domain.payment.PaymentDeclinedEvent;
import com.unisono.card.events.domain.payment.PaymentInitiatedEvent;
import com.unisono.card.events.domain.payment.PaymentProcessedEvent;
import com.unisono.card.events.domain.payment.PaymentReversedEvent;
import com.unisono.card.events.domain.payment.RefundInitiatedEvent;
import com.unisono.card.events.domain.payment.RefundProcessedEvent;
import com.unisono.card.events.domain.payment.TransactionSettledEvent;
import com.unisono.card.payment.domain.event.PaymentAuthorized;
import com.unisono.card.payment.domain.event.PaymentDeclined;
import com.unisono.card.payment.domain.event.PaymentInitiated;
import com.unisono.card.payment.domain.event.PaymentProcessed;
import com.unisono.card.payment.domain.event.PaymentReversed;
import com.unisono.card.payment.domain.event.RefundInitiated;
import com.unisono.card.payment.domain.event.RefundProcessed;
import com.unisono.card.payment.domain.event.TransactionSettled;
import com.unisono.card.payment.domain.model.TransactionAmount;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Traduce los eventos del dominio de pagos (con Value Objects) a los contratos de integración de card-events
 * (campos planos), que son los que se publican en Kafka. Conserva el eventId y el occurredOn del evento de
 * dominio, así que la deduplicación por eventId no cambia.
 * <p>
 * Montos: {@code amount}/{@code currency} es lo que se le cobra a la tarjeta (el monto efectivo, en la moneda
 * local si hubo conversión); {@code originalAmount}, {@code originalCurrency} y {@code exchangeRate} solo se
 * informan si hubo conversión.
 */
@Component
public class PaymentIntegrationEventMapper {

    public DomainEvent toIntegrationEvent(DomainEvent event) {
        if (event instanceof PaymentInitiated e) {
            Amount a = Amount.of(e.getAmount());
            return new PaymentInitiatedEvent(e.getEventId(), e.getOccurredOn(), id(e.getTransactionId()), card(e.getCardId()),
                    e.getType() != null ? e.getType().name() : null,
                    a.amount, a.currency, a.originalAmount, a.originalCurrency, a.exchangeRate, e.getInitiatedAt());
        }
        if (event instanceof PaymentAuthorized e) {
            return new PaymentAuthorizedEvent(e.getEventId(), e.getOccurredOn(), id(e.getTransactionId()), card(e.getCardId()),
                    e.getAuthorizationCode(), e.getRiskScore(), e.getAuthorizedAt());
        }
        if (event instanceof PaymentProcessed e) {
            Amount a = Amount.of(e.getAmount());
            return new PaymentProcessedEvent(e.getEventId(), e.getOccurredOn(), id(e.getTransactionId()), card(e.getCardId()),
                    e.getStatus() != null ? e.getStatus().name() : null,
                    a.amount, a.currency, a.originalAmount, a.originalCurrency, a.exchangeRate, e.getProcessedAt());
        }
        if (event instanceof PaymentDeclined e) {
            return new PaymentDeclinedEvent(e.getEventId(), e.getOccurredOn(), id(e.getTransactionId()), card(e.getCardId()),
                    e.getResponseCode(), e.getDeclineReason(), e.getDeclinedAt());
        }
        if (event instanceof PaymentReversed e) {
            Amount a = Amount.of(e.getAmount());
            return new PaymentReversedEvent(e.getEventId(), e.getOccurredOn(), id(e.getTransactionId()), card(e.getCardId()),
                    a.amount, a.currency, a.originalAmount, a.originalCurrency, a.exchangeRate,
                    e.getReversalReason(), e.getReversedAt());
        }
        if (event instanceof RefundInitiated e) {
            Amount a = Amount.of(e.getAmount());
            return new RefundInitiatedEvent(e.getEventId(), e.getOccurredOn(), id(e.getRefundTransactionId()),
                    id(e.getOriginalTransactionId()), card(e.getCardId()),
                    a.amount, a.currency, a.originalAmount, a.originalCurrency, a.exchangeRate, e.getInitiatedAt());
        }
        if (event instanceof RefundProcessed e) {
            Amount a = Amount.of(e.getAmount());
            return new RefundProcessedEvent(e.getEventId(), e.getOccurredOn(), id(e.getRefundTransactionId()),
                    id(e.getOriginalTransactionId()), card(e.getCardId()),
                    a.amount, a.currency, a.originalAmount, a.originalCurrency, a.exchangeRate, e.getProcessedAt());
        }
        if (event instanceof TransactionSettled e) {
            Amount a = Amount.of(e.getAmount());
            return new TransactionSettledEvent(e.getEventId(), e.getOccurredOn(), id(e.getTransactionId()), card(e.getCardId()),
                    a.amount, a.currency, a.originalAmount, a.originalCurrency, a.exchangeRate, e.getSettledAt());
        }
        throw new IllegalArgumentException("No integration contract for payment event " + event.getEventType());
    }

    private static String id(TransactionId transactionId) {
        return transactionId != null ? transactionId.toString() : null;
    }

    private static String card(CardId cardId) {
        return cardId != null ? cardId.getValue() : null;
    }

    private record Amount(BigDecimal amount, String currency, BigDecimal originalAmount, String originalCurrency,
                          BigDecimal exchangeRate) {

        static Amount of(TransactionAmount amount) {
            if (amount == null) {
                return new Amount(null, null, null, null, null);
            }
            Money effective = amount.getEffectiveAmount();
            if (!amount.hasCurrencyConversion()) {
                return new Amount(effective.getAmount(), effective.getCurrencyCode(), null, null, null);
            }
            Money original = amount.getAmount();
            return new Amount(effective.getAmount(), effective.getCurrencyCode(),
                    original.getAmount(), original.getCurrencyCode(), amount.getExchangeRate());
        }
    }
}
