package com.unisono.card.shared.domain.valueobject;

import com.unisono.card.shared.domain.exception.InvalidValueObjectException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Value Object que representa el PIN de la tarjeta.
 * El PIN NUNCA se almacena en texto plano, solo su hash BCrypt.
 * Este VO representa el PIN en texto plano (solo en memoria).
 */
@Getter
@EqualsAndHashCode
public class PIN implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int PIN_LENGTH = 4;

    private final String value;

    private PIN(String value) {
        this.value = value;
    }

    public static PIN of(String value) {
        validate(value);
        return new PIN(value);
    }

    private static void validate(String value) {
        if (StringUtils.isBlank(value)) {
            throw new InvalidValueObjectException("El PIN no puede ser nulo o vacío");
        }

        if (!value.matches("\\d+")) {
            throw new InvalidValueObjectException("El PIN debe contener solo dígitos");
        }

        if (value.length() != PIN_LENGTH) {
            throw new InvalidValueObjectException(
                    String.format("El PIN debe tener exactamente %d dígitos", PIN_LENGTH)
            );
        }

        // Validar que no sea secuencial (ej: 1234, 4321)
        if (isSequential(value)) {
            throw new InvalidValueObjectException("El PIN no puede ser una secuencia obvia (ej: 1234)");
        }

        // Validar que no sean todos iguales (ej: 1111)
        if (isAllSameDigit(value)) {
            throw new InvalidValueObjectException("El PIN no puede tener todos los dígitos iguales");
        }
    }

    private static boolean isSequential(String pin) {
        // Ascendente: 1234, 2345, etc.
        boolean ascending = true;
        for (int i = 0; i < pin.length() - 1; i++) {
            if (pin.charAt(i + 1) != pin.charAt(i) + 1) {
                ascending = false;
                break;
            }
        }

        // Descendente: 4321, 5432, etc.
        boolean descending = true;
        for (int i = 0; i < pin.length() - 1; i++) {
            if (pin.charAt(i + 1) != pin.charAt(i) - 1) {
                descending = false;
                break;
            }
        }

        return ascending || descending;
    }

    private static boolean isAllSameDigit(String pin) {
        char firstDigit = pin.charAt(0);
        for (int i = 1; i < pin.length(); i++) {
            if (pin.charAt(i) != firstDigit) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "****"; // NUNCA mostrar el PIN
    }
}

