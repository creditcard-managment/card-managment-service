package com.unisono.card.shared.domain.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

@Getter
@EqualsAndHashCode
public class Money implements Serializable {

    private static final long serialVersionUID = 1L;

    private final BigDecimal amount;
    private final Currency currency;

    // ========================================
    // Constructores privados
    // ========================================

    private Money(BigDecimal amount, Currency currency) {
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
    }

    // ========================================
    // Factory methods
    // ========================================

    /**
     * ⭐ NUEVO: Acepta String de currency (para JPA)
     */
    public static Money of(BigDecimal amount, String currencyCode) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new IllegalArgumentException("Currency code cannot be null or blank");
        }

        Currency currency = Currency.getInstance(currencyCode);
        return new Money(amount, currency);
    }

    /**
     * Acepta Currency object
     */
    public static Money of(BigDecimal amount, Currency currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }

        return new Money(amount, currency);
    }

    /**
     * Crea Money en ARS (Argentina)
     */
    public static Money ofARS(BigDecimal amount) {
        return of(amount, "ARS");
    }

    /**
     * Sobrecarga para long/int
     */
    public static Money ofARS(long amount) {
        return ofARS(BigDecimal.valueOf(amount));
    }

    /**
     * Money con valor cero
     */
    public static Money zero() {
        return ofARS(BigDecimal.ZERO);
    }

    /**
     * Money con valor cero en una moneda específica
     */
    public static Money zero(String currencyCode) {
        return of(BigDecimal.ZERO, currencyCode);
    }

    // ========================================
    // Getters para JPA (retornan String)
    // ========================================

    /**
     * ⭐ NUEVO: Retorna el código de currency como String (para JPA)
     */
    public String getCurrencyCode() {
        return currency.getCurrencyCode();
    }

    // ========================================
    // Business logic (operaciones)
    // ========================================

    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier), this.currency);
    }

    public Money divide(BigDecimal divisor) {
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Cannot divide by zero");
        }
        return new Money(this.amount.divide(divisor, 2, RoundingMode.HALF_UP), this.currency);
    }

    // ========================================
    // Comparisons
    // ========================================

    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isLessThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    // ========================================
    // Validations
    // ========================================

    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    String.format("Cannot operate on different currencies: %s and %s",
                            this.currency.getCurrencyCode(),
                            other.currency.getCurrencyCode())
            );
        }
    }

    // ========================================
    // Object methods
    // ========================================

    @Override
    public String toString() {
        return String.format("%s %s", currency.getSymbol(), amount);
    }
}
