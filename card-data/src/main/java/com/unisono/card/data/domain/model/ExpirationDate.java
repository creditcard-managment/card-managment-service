package com.unisono.card.data.domain.model;

import com.unisono.card.shared.domain.exception.ValidationException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Value Object que representa la fecha de vencimiento de la tarjeta.
 * Formato: MM/YY
 */
@Getter
@EqualsAndHashCode
public class ExpirationDate implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM/yy");

    private final YearMonth yearMonth;

    private ExpirationDate(YearMonth yearMonth) {
        this.yearMonth = yearMonth;
    }

    /**
     * Crea una ExpirationDate a partir de un YearMonth
     */
    public static ExpirationDate of(YearMonth yearMonth) {
        validate(yearMonth);
        return new ExpirationDate(yearMonth);
    }

    /**
     * Crea una ExpirationDate a partir de mes y año
     */
    public static ExpirationDate of(int month, int year) {
        YearMonth ym = YearMonth.of(year, month);
        return of(ym);
    }

    /**
     * Crea una ExpirationDate a partir de un string (formato: MM/YY)
     */
    public static ExpirationDate parse(String dateString) {
        try {
            YearMonth ym = YearMonth.parse(dateString, FORMATTER);
            return of(ym);
        } catch (Exception e) {
            throw new ValidationException("Formato de fecha inválido. Use MM/YY");
        }
    }

    /**
     * Genera una fecha de vencimiento por defecto (3 años desde hoy)
     */
    public static ExpirationDate generateDefault() {
        YearMonth threeYearsLater = YearMonth.now().plusYears(3);
        return of(threeYearsLater);
    }

    private static void validate(YearMonth yearMonth) {
        if (yearMonth == null) {
            throw new ValidationException("La fecha de vencimiento no puede ser nula");
        }

        YearMonth now = YearMonth.now();
        if (yearMonth.isBefore(now)) {
            throw new ValidationException("La tarjeta está vencida");
        }

        // No permitir vencimientos mayores a 10 años
        YearMonth maxDate = now.plusYears(10);
        if (yearMonth.isAfter(maxDate)) {
            throw new ValidationException("La fecha de vencimiento no puede ser mayor a 10 años");
        }
    }

    /**
     * Verifica si la tarjeta está vencida
     */
    public boolean isExpired() {
        return yearMonth.isBefore(YearMonth.now());
    }

    /**
     * Verifica si la tarjeta vence en los próximos N meses
     */
    public boolean expiresWithinMonths(int months) {
        YearMonth threshold = YearMonth.now().plusMonths(months);
        return yearMonth.isBefore(threshold) || yearMonth.equals(threshold);
    }

    /**
     * Retorna el último día del mes de vencimiento
     */
    public LocalDate getLastDayOfMonth() {
        return yearMonth.atEndOfMonth();
    }

    @Override
    public String toString() {
        return yearMonth.format(FORMATTER);
    }
}
