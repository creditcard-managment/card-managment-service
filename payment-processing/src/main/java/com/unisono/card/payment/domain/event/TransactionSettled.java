package com.unisono.card.payment.domain.event;

import com.unisono.card.events.domain.DomainEvent;
import com.unisono.card.payment.domain.model.TransactionAmount;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Event published when a transaction has been settled (liquidated)
 */
@Getter
public class TransactionSettled extends DomainEvent {

    private final TransactionId transactionId;
    private final CardId cardId;
    private final TransactionAmount amount;
    private final LocalDateTime settledAt;

    public TransactionSettled(TransactionId transactionId, CardId cardId,
                              TransactionAmount amount, LocalDateTime settledAt) {
        super();
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.amount = amount;
        this.settledAt = settledAt;
    }

    public TransactionSettled(String eventId, LocalDateTime occurredOn,
                              TransactionId transactionId, CardId cardId,
                              TransactionAmount amount, LocalDateTime settledAt) {
        super(eventId, occurredOn);
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.amount = amount;
        this.settledAt = settledAt;
    }

    @Override
    public String toString() {
        return String.format("TransactionSettled[transactionId=%s, amount=%s, settledAt=%s]",
                transactionId, amount, settledAt);
    }
}
