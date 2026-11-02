package com.unisono.card.payment.domain.exception;

import com.unisono.card.shared.domain.exception.DomainException;

/**
 * Base exception for payment processing domain
 */
public abstract class PaymentException extends DomainException {

    protected PaymentException(String message) {
        super(message);
    }

    protected PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
