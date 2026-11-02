package com.unisono.card.data.domain.port.input;

import com.unisono.card.shared.domain.valueobject.CardId;

/**
 * Use Case: Validar PIN de una tarjeta
 */
public interface ValidatePINUseCase {

    /**
     * Valida si el PIN proporcionado es correcto
     *
     * @param command datos para validar PIN
     * @return true si el PIN es correcto
     */
    boolean execute(ValidatePINCommand command);

    /**
     * Command para validar PIN
     */
    record ValidatePINCommand(
            CardId cardId,
            String plainPin
    ) {}
}
