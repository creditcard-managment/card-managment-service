package com.unisono.card.limits.domain.model;

import com.unisono.card.shared.domain.exception.ValidationException;
import com.unisono.card.shared.domain.valueobject.Money;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

/**
 * Value Object que representa el crédito consumido actual
 */
@Getter
@EqualsAndHashCode
public class ConsumedCredit implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Money amount;

    private ConsumedCredit(Money amount) {
        this.amount = amount;
    }

    public static ConsumedCredit of(Money amount) {
        validate(amount);
        return new ConsumedCredit(amount);
    }

    public static ConsumedCredit zero() {
        return of(Money.zero());
    }

    private static void validate(Money amount) {
        if (amount == null) {
            throw new ValidationException("El crédito consumido no puede ser nulo");
        }

        // No puede ser negativo (pero puede ser cero)
    }

    /**
     * Consume más crédito (suma al consumido)
     */
    public ConsumedCredit consume(Money additionalAmount) {
        Money newAmount = this.amount.add(additionalAmount);
        return of(newAmount);
    }

    /**
     * Libera crédito (resta del consumido, ej: después de un pago)
     */
    public ConsumedCredit release(Money releaseAmount) {
        Money newAmount = this.amount.subtract(releaseAmount);
        return of(newAmount);
    }

    @Override
    public String toString() {
        return amount.toString();
    }
}
