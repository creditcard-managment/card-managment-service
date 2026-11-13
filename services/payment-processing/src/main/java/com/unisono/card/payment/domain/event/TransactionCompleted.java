package com.unisono.card.payment.domain.event;

import com.unisono.card.events.domain.DomainEvent;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.model.TransactionStatus;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Event published when a transaction lifecycle is completed
 */
@Getter
public class TransactionCompleted extends DomainEvent implements PaymentEvent {

    private final TransactionId transactionId;
    private final CardId cardId;
    private final TransactionStatus finalStatus;
    private final LocalDateTime completedAt;

    public TransactionCompleted(TransactionId transactionId, CardId cardId,
                                TransactionStatus finalStatus, LocalDateTime completedAt) {
        super();
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.finalStatus = finalStatus;
        this.completedAt = completedAt;
    }

    public TransactionCompleted(String eventId, LocalDateTime occurredOn,
                                TransactionId transactionId, CardId cardId,
                                TransactionStatus finalStatus, LocalDateTime completedAt) {
        super(eventId, occurredOn);
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.finalStatus = finalStatus;
        this.completedAt = completedAt;
    }

    public boolean wasSuccessful() {
        return finalStatus == TransactionStatus.APPROVED || finalStatus == TransactionStatus.SETTLED;
    }

    @Override
    public String toString() {
        return String.format("TransactionCompleted[transactionId=%s, status=%s]",
                transactionId, finalStatus);
    }
}
