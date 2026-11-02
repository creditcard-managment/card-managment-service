package com.unisono.card.data.domain.port.input;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.shared.domain.valueobject.CardId;

/**
 * Use Case: Resetear PIN de una tarjeta
 */
public interface ResetPINUseCase {

    /**
     * Resetea el PIN de una tarjeta
     *
     * @param command datos para resetear PIN
     * @return la tarjeta con PIN actualizado
     */
    Card execute(ResetPINCommand command);

    /**
     * Command para resetear PIN
     */
    record ResetPINCommand(
            CardId cardId,
            String newPlainPin,
            String oldPlainPin  // Para validar que el usuario conoce el PIN actual
    ) {}
}
