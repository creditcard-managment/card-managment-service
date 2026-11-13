package com.unisono.card.payment.domain.event;

import com.unisono.card.events.domain.DomainEvent;
import com.unisono.card.payment.domain.model.TransactionAmount;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.model.TransactionStatus;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Event published when a payment has been processed (approved)
 */
@Getter
public class PaymentProcessed extends DomainEvent implements PaymentEvent {

    private final TransactionId transactionId;
    private final CardId cardId;
    private final TransactionAmount amount;
    private final TransactionStatus status;
    private final LocalDateTime processedAt;

    public PaymentProcessed(TransactionId transactionId, CardId cardId,
                            TransactionAmount amount, TransactionStatus status,
                            LocalDateTime processedAt) {
        super();
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.amount = amount;
        this.status = status;
        this.processedAt = processedAt;
    }

    public PaymentProcessed(String eventId, LocalDateTime occurredOn,
                            TransactionId transactionId, CardId cardId,
                            TransactionAmount amount, TransactionStatus status,
                            LocalDateTime processedAt) {
        super(eventId, occurredOn);
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.amount = amount;
        this.status = status;
        this.processedAt = processedAt;
    }

    @Override
    public String toString() {
        return String.format("PaymentProcessed[transactionId=%s, amount=%s, status=%s]",
                transactionId, amount, status);
    }
}
