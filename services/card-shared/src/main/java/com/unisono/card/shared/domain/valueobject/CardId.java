package com.unisono.card.shared.domain.valueobject;

import com.unisono.card.shared.domain.exception.InvalidValueObjectException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Value Object que representa el identificador único de una tarjeta.
 * Inmutable y con validación.
 */
@Getter
@EqualsAndHashCode
public class CardId implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final String value;

    private CardId(String value) {
        this.value = value;
    }

    /**
     * Crea un CardId a partir de un String.
     * Valida que sea un UUID en formato canónico (8-4-4-4-12) y lo normaliza a minúsculas.
     */
    public static CardId of(String value) {
        validate(value);
        return new CardId(value.toLowerCase(Locale.ROOT));
    }


    /**
     * Genera un nuevo CardId aleatorio usando UUID.
     */
    public static CardId generate() {
        return new CardId(UUID.randomUUID().toString());
    }

    private static void validate(String value) {
        if (StringUtils.isBlank(value)) {
            throw new InvalidValueObjectException("CardId no puede ser nulo o vacío");
        }
        if (!UUID_PATTERN.matcher(value).matches()) {
            throw new InvalidValueObjectException("CardId debe ser un UUID válido: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
