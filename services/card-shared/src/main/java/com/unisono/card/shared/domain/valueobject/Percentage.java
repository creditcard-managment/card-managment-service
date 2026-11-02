package com.unisono.card.shared.domain.valueobject;

import com.unisono.card.shared.domain.exception.InvalidValueObjectException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value Object que representa un porcentaje.
 * Útil para tasas de interés, comisiones, etc.
 */
@Getter
@EqualsAndHashCode
public class Percentage implements Serializable, Comparable<Percentage> {

    private static final long serialVersionUID = 1L;
    private static final int SCALE = 4; // 4 decimales para precisión
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final BigDecimal value; // Almacenado como decimal (ej: 0.15 = 15%)

    private Percentage(BigDecimal value) {
        this.value = value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Crea un Percentage a partir de un valor decimal (0.15 = 15%)
     */
    public static Percentage of(BigDecimal value) {
        validate(value, BigDecimal.ONE);
        return new Percentage(value);
    }

    /**
     * Crea un Percentage a partir de un porcentaje (15 = 15%)
     */
    public static Percentage fromPercentage(BigDecimal percentage) {
        validate(percentage, ONE_HUNDRED);
        return new Percentage(percentage.divide(ONE_HUNDRED, SCALE, RoundingMode.HALF_UP));
    }

    /**
     * Crea un Percentage a partir de un double (0.15 = 15%)
     */
    public static Percentage of(double value) {
        return of(BigDecimal.valueOf(value));
    }

    public static Percentage zero() {
        return of(BigDecimal.ZERO);
    }

    private static void validate(BigDecimal value, BigDecimal max) {
        if (value == null) {
            throw new InvalidValueObjectException("El porcentaje no puede ser nulo");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidValueObjectException("El porcentaje no puede ser negativo");
        }
        if (value.compareTo(max) > 0) {
            throw new InvalidValueObjectException("El porcentaje no puede ser mayor a 100%");
        }
    }

    /**
     * Aplica el porcentaje a un monto
     */
    public Money applyTo(Money money) {
        return money.multiply(this.value);
    }

    /**
     * Retorna el valor como porcentaje (0.15 -> 15.0)
     */
    public BigDecimal asPercentage() {
        return value.multiply(BigDecimal.valueOf(100));
    }

    @Override
    public int compareTo(Percentage other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return asPercentage() + "%";
    }
}
