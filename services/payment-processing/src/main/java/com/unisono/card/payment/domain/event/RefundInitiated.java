package com.unisono.card.payment.domain.event;

import com.unisono.card.events.domain.DomainEvent;
import com.unisono.card.payment.domain.model.TransactionAmount;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Event published when a refund is initiated
 */
@Getter
public class RefundInitiated extends DomainEvent implements PaymentEvent {

    private final TransactionId refundTransactionId;
    private final TransactionId originalTransactionId;
    private final CardId cardId;
    private final TransactionAmount amount;
    private final LocalDateTime initiatedAt;

    public RefundInitiated(TransactionId refundTransactionId, TransactionId originalTransactionId,
                           CardId cardId, TransactionAmount amount, LocalDateTime initiatedAt) {
        super();
        this.refundTransactionId = refundTransactionId;
        this.originalTransactionId = originalTransactionId;
        this.cardId = cardId;
        this.amount = amount;
        this.initiatedAt = initiatedAt;
    }

    public RefundInitiated(String eventId, LocalDateTime occurredOn,
                           TransactionId refundTransactionId, TransactionId originalTransactionId,
                           CardId cardId, TransactionAmount amount, LocalDateTime initiatedAt) {
        super(eventId, occurredOn);
        this.refundTransactionId = refundTransactionId;
        this.originalTransactionId = originalTransactionId;
        this.cardId = cardId;
        this.amount = amount;
        this.initiatedAt = initiatedAt;
    }

    @Override
    public String toString() {
        return String.format("RefundInitiated[refundId=%s, originalId=%s, amount=%s]",
                refundTransactionId, originalTransactionId, amount);
    }
}
