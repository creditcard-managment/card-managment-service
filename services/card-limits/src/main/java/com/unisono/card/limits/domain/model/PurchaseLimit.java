package com.unisono.card.limits.domain.model;

import com.unisono.card.shared.domain.exception.ValidationException;
import com.unisono.card.shared.domain.valueobject.Money;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

/**
 * Value Object que representa el límite de compras
 */
@Getter
@EqualsAndHashCode
public class PurchaseLimit implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Money MIN_LIMIT = Money.ofARS(1000);      // $1,000 mínimo
    private static final Money MAX_LIMIT = Money.ofARS(10_000_000); // $10M máximo

    private final Money amount;

    private PurchaseLimit(Money amount) {
        this.amount = amount;
    }

    public static PurchaseLimit of(Money amount) {
        validate(amount);
        return new PurchaseLimit(amount);
    }

    public static PurchaseLimit zero() {
        return of(Money.zero());
    }

    private static void validate(Money amount) {
        if (amount == null) {
            throw new ValidationException("El límite de compras no puede ser nulo");
        }

        if (amount.isLessThan(MIN_LIMIT)) {
            throw new ValidationException(
                    String.format("El límite de compras debe ser al menos %s", MIN_LIMIT)
            );
        }

        if (amount.isGreaterThan(MAX_LIMIT)) {
            throw new ValidationException(
                    String.format("El límite de compras no puede exceder %s", MAX_LIMIT)
            );
        }
    }

    /**
     * Aumenta el límite
     */
    public PurchaseLimit increase(Money additionalAmount) {
        Money newAmount = this.amount.add(additionalAmount);
        return of(newAmount);
    }

    /**
     * Disminuye el límite
     */
    public PurchaseLimit decrease(Money reductionAmount) {
        Money newAmount = this.amount.subtract(reductionAmount);
        return of(newAmount);
    }

    /**
     * Verifica si un monto está dentro del límite
     */
    public boolean allows(Money purchaseAmount) {
        return !purchaseAmount.isGreaterThan(this.amount);
    }

    @Override
    public String toString() {
        return amount.toString();
    }
}
