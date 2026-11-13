package com.unisono.card.payment.domain.event;

import com.unisono.card.events.domain.DomainEvent;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Event published when a payment is declined
 */
@Getter
public class PaymentDeclined extends DomainEvent implements PaymentEvent {

    private final TransactionId transactionId;
    private final CardId cardId;
    private final String declineReason;
    private final LocalDateTime declinedAt;

    public PaymentDeclined(TransactionId transactionId, CardId cardId,
                           String declineReason, LocalDateTime declinedAt) {
        super();
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.declineReason = declineReason;
        this.declinedAt = declinedAt;
    }

    public PaymentDeclined(String eventId, LocalDateTime occurredOn,
                           TransactionId transactionId, CardId cardId,
                           String declineReason, LocalDateTime declinedAt) {
        super(eventId, occurredOn);
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.declineReason = declineReason;
        this.declinedAt = declinedAt;
    }

    @Override
    public String toString() {
        return String.format("PaymentDeclined[transactionId=%s, reason=%s]",
                transactionId, declineReason);
    }
}
