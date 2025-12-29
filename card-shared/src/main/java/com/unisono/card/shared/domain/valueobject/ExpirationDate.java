package com.unisono.card.shared.domain.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Value Object: Fecha de expiración de la tarjeta
 */
@Getter // ✅ IMPORTANTE: Esto genera getMonth() y getYear()
@EqualsAndHashCode
public class ExpirationDate {

    private final int month; // ✅ Generará getMonth()
    private final int year;  // ✅ Generará getYear()

    private ExpirationDate(int month, int year) {
        validate(month, year);
        this.month = month;
        this.year = year;
    }

    public static ExpirationDate of(int month, int year) {
        return new ExpirationDate(month, year);
    }

    /**
     * Crea desde String formato "MM/YY" o "MM/YYYY"
     */
    public static ExpirationDate fromString(String value) {
        Objects.requireNonNull(value, "Fecha de expiración no puede ser null");

        String[] parts = value.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "Formato inválido. Use MM/YY o MM/YYYY"
            );
        }

        int month = Integer.parseInt(parts[0].trim());
        int year = Integer.parseInt(parts[1].trim());

        // Si el año es de 2 dígitos, convertir a 4 dígitos
        if (year < 100) {
            year += 2000;
        }

        return new ExpirationDate(month, year);
    }

    public YearMonth getYearMonth() {
        // Usamos el método estático .of() en lugar de 'new'
        return YearMonth.of(this.getYear(), this.getMonth());
    }

    private void validate(int month, int year) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException(
                    "Mes debe estar entre 1 y 12. Valor recibido: " + month
            );
        }

        int currentYear = LocalDate.now().getYear();
        if (year < currentYear) {
            throw new IllegalArgumentException(
                    "Año no puede ser anterior al año actual. Año recibido: " + year
            );
        }

        if (year > currentYear + 10) {
            throw new IllegalArgumentException(
                    "Año no puede ser más de 10 años en el futuro. Año recibido: " + year
            );
        }
    }

    /**
     * Verifica si la tarjeta está vencida
     */
    public boolean isExpired() {
        YearMonth expirationYearMonth = YearMonth.of(year, month);
        YearMonth currentYearMonth = YearMonth.now();

        return expirationYearMonth.isBefore(currentYearMonth);
    }

    /**
     * Verifica si la tarjeta vence en los próximos N meses
     */
    public boolean expiresWithinMonths(int months) {
        YearMonth expirationYearMonth = YearMonth.of(year, month);
        YearMonth futureYearMonth = YearMonth.now().plusMonths(months);

        return expirationYearMonth.isBefore(futureYearMonth)
                || expirationYearMonth.equals(futureYearMonth);
    }

    /**
     * Obtiene el último día del mes de expiración
     */
    public LocalDate getLastDayOfMonth() {
        return YearMonth.of(year, month).atEndOfMonth();
    }

    @Override
    public String toString() {
        return String.format("%02d/%04d", month, year);
    }

    /**
     * Formato corto (MM/YY)
     */
    public String toShortString() {
        return String.format("%02d/%02d", month, year % 100);
    }
}
