package com.unisono.card.limits.domain.model;

import com.unisono.card.shared.domain.exception.ValidationException;
import com.unisono.card.shared.domain.valueobject.Money;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Value Object que representa el límite diario de transacciones
 * Se resetea automáticamente cada día
 */
@Getter
@EqualsAndHashCode
public class DailyLimit implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Money DEFAULT_DAILY_LIMIT = Money.ofARS(100_000); // $100k por día

    private final Money amount;
    private final Money consumed;
    private final LocalDate resetDate; // Fecha del último reset

    private DailyLimit(Money amount, Money consumed, LocalDate resetDate) {
        this.amount = amount;
        this.consumed = consumed;
        this.resetDate = resetDate;
    }

    /**
     * Crea un nuevo límite diario
     */
    public static DailyLimit of(Money amount) {
        validate(amount);
        return new DailyLimit(amount, Money.zero(), LocalDate.now());
    }

    /**
     * Crea un límite diario por defecto
     */
    public static DailyLimit defaultLimit() {
        return of(DEFAULT_DAILY_LIMIT);
    }

    /**
     * Reconstruye desde persistencia
     */
    public static DailyLimit reconstituteFromPersistence(
            Money amount,
            Money consumed,
            LocalDate resetDate
    ) {
        validate(amount);
        return new DailyLimit(amount, consumed, resetDate);
    }

    private static void validate(Money amount) {
        if (amount == null) {
            throw new ValidationException("El límite diario no puede ser nulo");
        }
    }

    /**
     * Consume parte del límite diario
     */
    public DailyLimit consume(Money transactionAmount) {
        // Auto-reset si cambió el día
        if (needsReset()) {
            return resetAndConsume(transactionAmount);
        }

        Money newConsumed = this.consumed.add(transactionAmount);

        // Validar que no exceda el límite
        if (newConsumed.isGreaterThan(this.amount)) {
            throw new ValidationException(
                    String.format("Se excedió el límite diario. Disponible: %s, Solicitado: %s",
                            getAvailable(), transactionAmount)
            );
        }

        return new DailyLimit(this.amount, newConsumed, this.resetDate);
    }

    /**
     * Verifica si necesita reset (cambió el día)
     */
    public boolean needsReset() {
        return !this.resetDate.equals(LocalDate.now());
    }

    /**
     * Resetea el límite diario y consume el monto
     */
    private DailyLimit resetAndConsume(Money transactionAmount) {
        if (transactionAmount.isGreaterThan(this.amount)) {
            throw new ValidationException(
                    String.format("La transacción excede el límite diario de %s", this.amount)
            );
        }
        return new DailyLimit(this.amount, transactionAmount, LocalDate.now());
    }

    /**
     * Obtiene el monto disponible hoy
     */
    public Money getAvailable() {
        if (needsReset()) {
            return this.amount;
        }
        return this.amount.subtract(this.consumed);
    }

    /**
     * Verifica si un monto está dentro del límite diario disponible
     */
    public boolean allows(Money transactionAmount) {
        return !transactionAmount.isGreaterThan(getAvailable());
    }

    /**
     * Actualiza el límite diario (nuevo monto)
     */
    public DailyLimit updateLimit(Money newAmount) {
        validate(newAmount);
        // Mantiene el consumo actual si es del mismo día
        if (needsReset()) {
            return new DailyLimit(newAmount, Money.zero(), LocalDate.now());
        }
        return new DailyLimit(newAmount, this.consumed, this.resetDate);
    }

    @Override
    public String toString() {
        return String.format("DailyLimit{limit=%s, consumed=%s, available=%s, date=%s}",
                amount, consumed, getAvailable(), resetDate);
    }
}
