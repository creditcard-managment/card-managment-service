package com.unisono.card.payment.domain.service;

import com.unisono.card.payment.domain.exception.InvalidTransactionException;
import com.unisono.card.payment.domain.model.*;
import com.unisono.card.shared.domain.valueobject.Money;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * Domain service for transaction validation logic
 */
@Slf4j
public class TransactionValidationService {

    private static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("50000.00");
    private static final BigDecimal MIN_TRANSACTION_AMOUNT = new BigDecimal("0.01");

    /**
     * Validate transaction amount
     */
    public void validateAmount(TransactionAmount amount) {
        Money effectiveAmount = amount.getEffectiveAmount();

        if (effectiveAmount.getAmount().compareTo(MIN_TRANSACTION_AMOUNT) < 0) {
            throw new InvalidTransactionException(
                    "Transaction amount must be at least " + MIN_TRANSACTION_AMOUNT
            );
        }

        if (effectiveAmount.getAmount().compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            throw new InvalidTransactionException(
                    "Transaction amount exceeds maximum limit: " + MAX_TRANSACTION_AMOUNT
            );
        }
    }

    /**
     * Validate payment method for transaction type
     */
    public void validatePaymentMethod(PaymentMethod method, TransactionType type) {
        if (type == TransactionType.CASH_ADVANCE && method == PaymentMethod.ONLINE) {
            throw new InvalidTransactionException(
                    "Cash advances cannot be processed online"
            );
        }

        if (type == TransactionType.REFUND && !method.isCardPresent()) {
            log.warn("Refund processed without card present: {}", method);
        }
    }

    /**
     * Validate refund amount against original transaction
     */
    public void validateRefundAmount(Money refundAmount, Money originalAmount) {
        if (refundAmount.getAmount().compareTo(originalAmount.getAmount()) > 0) {
            throw new InvalidTransactionException(
                    String.format("Refund amount %s exceeds original amount %s",
                            refundAmount, originalAmount)
            );
        }
    }

    /**
     * Validate transaction can be reversed
     */
    public void validateCanReverse(Transaction transaction) {
        if (!transaction.getStatus().canBeReversed()) {
            throw new InvalidTransactionException(
                    transaction.getTransactionId(),
                    String.format("Transaction with status %s cannot be reversed",
                            transaction.getStatus())
            );
        }
    }
}
