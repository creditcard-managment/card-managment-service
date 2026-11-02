package com.unisono.card.payment.domain.model;

import com.unisono.card.shared.domain.exception.InvalidValueObjectException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.util.UUID;

/**
 * Value Object representing a unique transaction identifier
 */
@Getter
@EqualsAndHashCode
public class TransactionId implements Serializable {

    private final UUID value;

    private TransactionId(UUID value) {
        if (value == null) {
            throw new InvalidValueObjectException("TransactionId cannot be null");
        }
        this.value = value;
    }

    public static TransactionId of(UUID value) {
        return new TransactionId(value);
    }

    public static TransactionId of(String value) {
        try {
            return new TransactionId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new InvalidValueObjectException("Invalid TransactionId format: " + value, e);
        }
    }

    public static TransactionId generate() {
        return new TransactionId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
