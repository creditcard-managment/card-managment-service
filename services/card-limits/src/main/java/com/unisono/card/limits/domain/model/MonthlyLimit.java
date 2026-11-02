package com.unisono.card.limits.domain.model;

import com.unisono.card.shared.domain.exception.ValidationException;
import com.unisono.card.shared.domain.valueobject.Money;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.time.YearMonth;

/**
 * Value Object que representa el límite mensual de transacciones
 * Se resetea automáticamente cada mes
 */
@Getter
@EqualsAndHashCode
public class MonthlyLimit implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Money DEFAULT_MONTHLY_LIMIT = Money.ofARS(500_000); // $500k por mes

    private final Money amount;
    private final Money consumed;
    private final YearMonth resetMonth; // Mes del último reset

    private MonthlyLimit(Money amount, Money consumed, YearMonth resetMonth) {
        this.amount = amount;
        this.consumed = consumed;
        this.resetMonth = resetMonth;
    }

    /**
     * Crea un nuevo límite mensual
     */
    public static MonthlyLimit of(Money amount) {
        validate(amount);
        return new MonthlyLimit(amount, Money.zero(), YearMonth.now());
    }

    /**
     * Crea un límite mensual por defecto
     */
    public static MonthlyLimit defaultLimit() {
        return of(DEFAULT_MONTHLY_LIMIT);
    }

    /**
     * Reconstruye desde persistencia
     */
    public static MonthlyLimit reconstituteFromPersistence(
            Money amount,
            Money consumed,
            YearMonth resetMonth
    ) {
        validate(amount);
        return new MonthlyLimit(amount, consumed, resetMonth);
    }

    private static void validate(Money amount) {
        if (amount == null) {
            throw new ValidationException("El límite mensual no puede ser nulo");
        }
    }

    /**
     * Consume parte del límite mensual
     */
    public MonthlyLimit consume(Money transactionAmount) {
        // Auto-reset si cambió el mes
        if (needsReset()) {
            return resetAndConsume(transactionAmount);
        }

        Money newConsumed = this.consumed.add(transactionAmount);

        // Validar que no exceda el límite
        if (newConsumed.isGreaterThan(this.amount)) {
            throw new ValidationException(
                    String.format("Se excedió el límite mensual. Disponible: %s, Solicitado: %s",
                            getAvailable(), transactionAmount)
            );
        }

        return new MonthlyLimit(this.amount, newConsumed, this.resetMonth);
    }

    /**
     * Verifica si necesita reset (cambió el mes)
     */
    public boolean needsReset() {
        return !this.resetMonth.equals(YearMonth.now());
    }

    /**
     * Resetea el límite mensual y consume el monto
     */
    private MonthlyLimit resetAndConsume(Money transactionAmount) {
        if (transactionAmount.isGreaterThan(this.amount)) {
            throw new ValidationException(
                    String.format("La transacción excede el límite mensual de %s", this.amount)
            );
        }
        return new MonthlyLimit(this.amount, transactionAmount, YearMonth.now());
    }

    /**
     * Obtiene el monto disponible este mes
     */
    public Money getAvailable() {
        if (needsReset()) {
            return this.amount;
        }
        return this.amount.subtract(this.consumed);
    }

    /**
     * Verifica si un monto está dentro del límite mensual disponible
     */
    public boolean allows(Money transactionAmount) {
        return !transactionAmount.isGreaterThan(getAvailable());
    }

    /**
     * Actualiza el límite mensual (nuevo monto)
     */
    public MonthlyLimit updateLimit(Money newAmount) {
        validate(newAmount);
        // Mantiene el consumo actual si es del mismo mes
        if (needsReset()) {
            return new MonthlyLimit(newAmount, Money.zero(), YearMonth.now());
        }
        return new MonthlyLimit(newAmount, this.consumed, this.resetMonth);
    }

    @Override
    public String toString() {
        return String.format("MonthlyLimit{limit=%s, consumed=%s, available=%s, month=%s}",
                amount, consumed, getAvailable(), resetMonth);
    }
}
