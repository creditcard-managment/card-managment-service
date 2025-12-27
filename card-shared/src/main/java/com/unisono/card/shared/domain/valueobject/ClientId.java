package com.unisono.card.shared.domain.valueobject;

import com.unisono.card.shared.domain.exception.ValidationException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Value Object que representa el identificador de un cliente.
 * En Argentina podría ser DNI, CUIL, CUIT, etc.
 */
@Getter
@EqualsAndHashCode
public class ClientId implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int MIN_LENGTH = 7;
    private static final int MAX_LENGTH = 11;

    private final String value;

    private ClientId(String value) {
        this.value = value;
    }

    public static ClientId of(String value) {
        validate(value);
        return new ClientId(sanitize(value));
    }

    private static void validate(String value) {
        if (StringUtils.isBlank(value)) {
            throw new ValidationException("ClientId no puede ser nulo o vacío");
        }

        String sanitized = sanitize(value);

        if (sanitized.length() < MIN_LENGTH || sanitized.length() > MAX_LENGTH) {
            throw new ValidationException(
                    String.format("ClientId debe tener entre %d y %d dígitos", MIN_LENGTH, MAX_LENGTH)
            );
        }

        if (!sanitized.matches("\\d+")) {
            throw new ValidationException("ClientId debe contener solo dígitos");
        }
    }

    /**
     * Remueve guiones y espacios (ej: "20-12345678-9" -> "20123456789")
     */
    private static String sanitize(String value) {
        return value.replaceAll("[^0-9]", "");
    }

    @Override
    public String toString() {
        return value;
    }
}
