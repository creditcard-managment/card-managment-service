package com.unisono.card.payment.domain.event;

import com.unisono.card.events.domain.DomainEvent;
import com.unisono.card.payment.domain.model.TransactionAmount;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Event published when a payment is reversed
 */
@Getter
public class PaymentReversed extends DomainEvent implements PaymentEvent {

    private final TransactionId transactionId;
    private final CardId cardId;
    private final TransactionAmount amount;
    private final String reversalReason;
    private final LocalDateTime reversedAt;

    public PaymentReversed(TransactionId transactionId, CardId cardId,
                           TransactionAmount amount, String reversalReason,
                           LocalDateTime reversedAt) {
        super();
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.amount = amount;
        this.reversalReason = reversalReason;
        this.reversedAt = reversedAt;
    }

    public PaymentReversed(String eventId, LocalDateTime occurredOn,
                           TransactionId transactionId, CardId cardId,
                           TransactionAmount amount, String reversalReason,
                           LocalDateTime reversedAt) {
        super(eventId, occurredOn);
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.amount = amount;
        this.reversalReason = reversalReason;
        this.reversedAt = reversedAt;
    }

    @Override
    public String toString() {
        return String.format("PaymentReversed[transactionId=%s, amount=%s, reason=%s]",
                transactionId, amount, reversalReason);
    }
}
