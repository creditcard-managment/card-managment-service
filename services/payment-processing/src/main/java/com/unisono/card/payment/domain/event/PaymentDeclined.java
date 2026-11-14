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
    /** ISO 8583 response code of the rejection (51 funds, 55 PIN, 59 fraud, ...) */
    private final String responseCode;
    private final String declineReason;
    private final LocalDateTime declinedAt;

    public PaymentDeclined(TransactionId transactionId, CardId cardId, String responseCode,
                           String declineReason, LocalDateTime declinedAt) {
        super();
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.responseCode = responseCode;
        this.declineReason = declineReason;
        this.declinedAt = declinedAt;
    }

    @Override
    public String toString() {
        return String.format("PaymentDeclined[transactionId=%s, responseCode=%s, reason=%s]",
                transactionId, responseCode, declineReason);
    }
}
