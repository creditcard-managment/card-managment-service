package com.unisono.card.data.domain.port.input;

import com.unisono.card.shared.domain.valueobject.CardId;

/**
 * Use Case: Validar el CVV de una tarjeta (pagos con tarjeta no presente)
 */
public interface ValidateCVVUseCase {

    /**
     * Valida si el CVV proporcionado coincide con el de la tarjeta
     *
     * @return true si el CVV es correcto; false si no coincide o la tarjeta no tiene CVV asignado
     */
    boolean execute(ValidateCVVCommand command);

    record ValidateCVVCommand(
            CardId cardId,
            String plainCvv
    ) {}
}
