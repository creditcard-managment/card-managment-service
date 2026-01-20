package com.unisono.card.shared.domain.valueobject;

import com.unisono.card.shared.domain.exception.ValidationException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.UUID;

/**
 * Value Object que representa el identificador único de una tarjeta.
 * Inmutable y con validación.
 */
@Getter
@EqualsAndHashCode
public class CardId implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String value;

    public CardId(String value) {
        this.value = value;
    }

    /**
     * Crea un CardId a partir de un String.
     * Valida que no sea nulo ni vacío.
     */
    public static CardId of(String value) {
        validate(value);
        return new CardId(value);
    }


    /**
     * Genera un nuevo CardId aleatorio usando UUID.
     */
    public static CardId generate() {
        return new CardId(UUID.randomUUID().toString());
    }

    private static void validate(String value) {
        if (StringUtils.isBlank(value)) {
            throw new ValidationException("CardId no puede ser nulo o vacío");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
