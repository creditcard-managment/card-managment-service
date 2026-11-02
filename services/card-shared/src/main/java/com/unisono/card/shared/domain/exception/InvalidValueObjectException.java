package com.unisono.card.shared.domain.exception;

/**
 * Exception thrown when a Value Object fails validation
 */
public class InvalidValueObjectException extends DomainException {

    public InvalidValueObjectException(String message) {
        super(message);
    }

    public InvalidValueObjectException(String message, Throwable cause) {
        super(message, cause);
    }
}
