package com.unisono.card.limits.domain.model;

import com.unisono.card.shared.domain.exception.ValidationException;
import com.unisono.card.shared.domain.valueobject.Money;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

/**
 * Value Object que representa el crédito disponible actual
 * Se calcula como: Límite Total - Crédito Consumido
 */
@Getter
@EqualsAndHashCode
public class AvailableCredit implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Money amount;

    private AvailableCredit(Money amount) {
        this.amount = amount;
    }

    public static AvailableCredit of(Money amount) {
        validate(amount);
        return new AvailableCredit(amount);
    }

    public static AvailableCredit zero() {
        return of(Money.zero());
    }

    /**
     * Calcula el crédito disponible a partir del límite total y consumido
     */
    public static AvailableCredit calculate(Money totalLimit, Money consumedCredit) {
        if (totalLimit == null || consumedCredit == null) {
            throw new ValidationException("Los montos no pueden ser nulos");
        }

        Money available = totalLimit.subtract(consumedCredit);
        return of(available);
    }

    private static void validate(Money amount) {
        if (amount == null) {
            throw new ValidationException("El crédito disponible no puede ser nulo");
        }

        // El crédito disponible puede ser cero, pero no negativo
        // (esto se validará en el aggregate)
    }

    /**
     * Verifica si hay suficiente crédito para un monto
     */
    public boolean isSufficientFor(Money requiredAmount) {
        return !requiredAmount.isGreaterThan(this.amount);
    }

    /**
     * Verifica si el crédito está agotado
     */
    public boolean isExhausted() {
        return this.amount.isZero();
    }

    @Override
    public String toString() {
        return amount.toString();
    }
}
