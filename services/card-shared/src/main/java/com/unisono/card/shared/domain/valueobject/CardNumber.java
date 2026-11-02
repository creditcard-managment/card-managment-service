package com.unisono.card.shared.domain.valueobject;

import com.unisono.card.shared.domain.exception.InvalidValueObjectException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Value Object que representa el número de tarjeta (PAN).
 * Valida formato y algoritmo de Luhn.
 * Este valor SIEMPRE debe estar encriptado en BD.
 */
@Getter
@EqualsAndHashCode
public class CardNumber implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int MIN_LENGTH = 13;
    private static final int MAX_LENGTH = 19;

    private final String value;

    private CardNumber(String value) {
        this.value = value;
    }

    /**
     * Crea un CardNumber a partir de un string.
     * Valida formato y Luhn checksum.
     */
    public static CardNumber of(String value) {
        validate(value);
        return new CardNumber(sanitize(value));
    }

    private static void validate(String value) {
        if (StringUtils.isBlank(value)) {
            throw new InvalidValueObjectException("El número de tarjeta no puede ser nulo o vacío");
        }

        String sanitized = sanitize(value);

        if (sanitized.length() < MIN_LENGTH || sanitized.length() > MAX_LENGTH) {
            throw new InvalidValueObjectException(
                    String.format("El número de tarjeta debe tener entre %d y %d dígitos",
                            MIN_LENGTH, MAX_LENGTH)
            );
        }

        if (!sanitized.matches("\\d+")) {
            throw new InvalidValueObjectException("El número de tarjeta debe contener solo dígitos");
        }

        if (!isValidLuhn(sanitized)) {
            throw new InvalidValueObjectException("El número de tarjeta no pasa la validación de Luhn");
        }
    }

    /**
     * Algoritmo de Luhn para validar números de tarjeta
     */
    private static boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return (sum % 10 == 0);
    }

    /**
     * Remueve espacios y guiones
     */
    private static String sanitize(String value) {
        return value.replaceAll("[\\s-]", "");
    }

    /**
     * Retorna los últimos 4 dígitos (para mostrar al usuario)
     */
    public String getLastFourDigits() {
        return value.substring(value.length() - 4);
    }

    /**
     * Retorna el número enmascarado: todos los dígitos salvo los últimos 4 como '*',
     * agrupados de a 4 desde la derecha (16 dígitos: **** **** **** 1234,
     * 15 dígitos: *** **** **** 1234, 19 dígitos: *** **** **** **** 1234)
     */
    public String getMasked() {
        String masked = "*".repeat(value.length() - 4) + getLastFourDigits();

        StringBuilder grouped = new StringBuilder();
        int firstGroup = masked.length() % 4;
        if (firstGroup > 0) {
            grouped.append(masked, 0, firstGroup);
        }
        for (int i = firstGroup; i < masked.length(); i += 4) {
            if (grouped.length() > 0) {
                grouped.append(' ');
            }
            grouped.append(masked, i, i + 4);
        }
        return grouped.toString();
    }

    /**
     * Retorna el BIN/IIN: los primeros 6 dígitos, que identifican al emisor
     */
    public String getBIN() {
        return value.substring(0, Math.min(6, value.length()));
    }

    @Override
    public String toString() {
        return getMasked(); // Nunca loguear el número completo
    }
}
