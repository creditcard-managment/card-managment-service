package com.unisono.card.shared.domain.valueobject;

import com.unisono.card.shared.domain.exception.ValidationException;
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

    public CardNumber(String value) {
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
            throw new ValidationException("El número de tarjeta no puede ser nulo o vacío");
        }

        String sanitized = sanitize(value);

        if (sanitized.length() < MIN_LENGTH || sanitized.length() > MAX_LENGTH) {
            throw new ValidationException(
                    String.format("El número de tarjeta debe tener entre %d y %d dígitos",
                            MIN_LENGTH, MAX_LENGTH)
            );
        }

        if (!sanitized.matches("\\d+")) {
            throw new ValidationException("El número de tarjeta debe contener solo dígitos");
        }

        if (!isValidLuhn(sanitized)) {
            throw new ValidationException("El número de tarjeta no pasa la validación de Luhn");
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
     * Retorna el número enmascarado (ej: **** **** **** 1234)
     */
    public String getMasked() {
        String lastFour = getLastFourDigits();
        return "**** **** **** " + lastFour;
    }

    /**
     * Determina el tipo de tarjeta por el BIN (primeros 6 dígitos)
     */
    public String getBIN() {
        return value.substring(0, Math.min(6, value.length()));
    }

    @Override
    public String toString() {
        return getMasked(); // Nunca loguear el número completo
    }
}
