package com.unisono.card.payment.domain.event;

import com.unisono.card.events.domain.DomainEvent;
import com.unisono.card.payment.domain.model.TransactionAmount;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.model.TransactionType;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Event published when a payment process is initiated
 */
@Getter
public class PaymentInitiated extends DomainEvent {

    private final TransactionId transactionId;
    private final CardId cardId;
    private final TransactionAmount amount;
    private final TransactionType type;
    private final LocalDateTime initiatedAt;

    public PaymentInitiated(TransactionId transactionId, CardId cardId,
                            TransactionAmount amount, TransactionType type,
                            LocalDateTime initiatedAt) {
        super();
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.amount = amount;
        this.type = type;
        this.initiatedAt = initiatedAt;
    }

    // Constructor para reconstruir desde persistencia
    public PaymentInitiated(String eventId, LocalDateTime occurredOn,
                            TransactionId transactionId, CardId cardId,
                            TransactionAmount amount, TransactionType type,
                            LocalDateTime initiatedAt) {
        super(eventId, occurredOn);
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.amount = amount;
        this.type = type;
        this.initiatedAt = initiatedAt;
    }

    @Override
    public String toString() {
        return String.format("PaymentInitiated[transactionId=%s, cardId=%s, amount=%s, type=%s]",
                transactionId, cardId, amount, type);
    }
}
