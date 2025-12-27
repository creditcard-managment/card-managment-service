package com.unisono.card.data.domain.model;


import com.unisono.card.shared.domain.exception.ValidationException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Value Object que representa el CVV de la tarjeta.
 * NUNCA debe ser mostrado completo al usuario.
 * SIEMPRE debe estar encriptado en BD.
 */
@Getter
@EqualsAndHashCode
public class CVV implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int CVV_LENGTH = 3;
    private static final int CVV_AMEX_LENGTH = 4; // American Express usa 4 dígitos

    private final String value;

    private CVV(String value) {
        this.value = value;
    }

    public static CVV of(String value) {
        validate(value);
        return new CVV(value);
    }

    private static void validate(String value) {
        if (StringUtils.isBlank(value)) {
            throw new ValidationException("El CVV no puede ser nulo o vacío");
        }

        if (!value.matches("\\d+")) {
            throw new ValidationException("El CVV debe contener solo dígitos");
        }

        if (value.length() != CVV_LENGTH && value.length() != CVV_AMEX_LENGTH) {
            throw new ValidationException(
                    String.format("El CVV debe tener %d o %d dígitos", CVV_LENGTH, CVV_AMEX_LENGTH)
            );
        }
    }

    @Override
    public String toString() {
        return "***"; // NUNCA mostrar el CVV real
    }
}
