package com.unisono.card.shared.domain.valueobject;

import com.unisono.card.shared.domain.exception.InvalidValueObjectException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Value Object: Fecha de expiración de la tarjeta
 *
 * of / fromString aceptan cualquier fecha bien formada, incluso vencida, para poder
 * reconstruir tarjetas desde persistencia o validar pagos con tarjetas vencidas.
 * forIssuance agrega la ventana de emisión (no vencida y a no más de 10 años).
 */
@Getter
@EqualsAndHashCode
public class ExpirationDate implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2099;
    private static final int MAX_YEARS_AHEAD = 10;

    private final int month;
    private final int year;

    private ExpirationDate(int month, int year) {
        validate(month, year);
        this.month = month;
        this.year = year;
    }

    public static ExpirationDate of(int month, int year) {
        return new ExpirationDate(month, year);
    }

    /**
     * Crea desde String formato "MM/YYYY"
     */
    public static ExpirationDate of(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidValueObjectException("Expiration date cannot be null or blank");
        }

        if (!value.matches("(0[1-9]|1[0-2])/20\\d{2}")) {
            throw new InvalidValueObjectException(
                    "Invalid expiration date format. Expected MM/YYYY, got: " + value
            );
        }

        String[] parts = value.split("/");
        return new ExpirationDate(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    /**
     * Crea desde String formato "MM/YY" o "MM/YYYY"
     */
    public static ExpirationDate fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidValueObjectException("Fecha de expiración no puede ser nula o vacía");
        }

        String[] parts = value.split("/");
        if (parts.length != 2) {
            throw new InvalidValueObjectException("Formato inválido. Use MM/YY o MM/YYYY");
        }

        try {
            int month = Integer.parseInt(parts[0].trim());
            int year = Integer.parseInt(parts[1].trim());

            // Si el año es de 2 dígitos, convertir a 4 dígitos
            if (year < 100) {
                year += 2000;
            }

            return new ExpirationDate(month, year);
        } catch (NumberFormatException e) {
            throw new InvalidValueObjectException("Formato inválido. Use MM/YY o MM/YYYY", e);
        }
    }

    /**
     * Crea una fecha válida para emitir una tarjeta: no vencida y a no más de 10 años
     */
    public static ExpirationDate forIssuance(int month, int year) {
        ExpirationDate date = new ExpirationDate(month, year);

        if (date.isExpired()) {
            throw new InvalidValueObjectException(
                    "La fecha de expiración " + date + " ya está vencida"
            );
        }

        if (year > LocalDate.now().getYear() + MAX_YEARS_AHEAD) {
            throw new InvalidValueObjectException(
                    String.format("El año de expiración no puede superar en más de %d al actual. Valor recibido: %s",
                            MAX_YEARS_AHEAD, date)
            );
        }

        return date;
    }

    public YearMonth getYearMonth() {
        return YearMonth.of(year, month);
    }

    private static void validate(int month, int year) {
        if (month < 1 || month > 12) {
            throw new InvalidValueObjectException(
                    "Mes debe estar entre 1 y 12. Valor recibido: " + month
            );
        }

        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new InvalidValueObjectException(
                    String.format("Año debe estar entre %d y %d. Valor recibido: %d", MIN_YEAR, MAX_YEAR, year)
            );
        }
    }

    /**
     * Verifica si la tarjeta está vencida
     */
    public boolean isExpired() {
        return getYearMonth().isBefore(YearMonth.now());
    }

    /**
     * Verifica si la tarjeta vence en los próximos N meses
     */
    public boolean expiresWithinMonths(int months) {
        return !getYearMonth().isAfter(YearMonth.now().plusMonths(months));
    }

    /**
     * Obtiene el último día del mes de expiración
     */
    public LocalDate getLastDayOfMonth() {
        return getYearMonth().atEndOfMonth();
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
