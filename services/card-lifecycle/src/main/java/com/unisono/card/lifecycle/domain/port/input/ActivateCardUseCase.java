package com.unisono.card.lifecycle.domain.port.input;

import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.PIN;

/**
 * Use Case: Activar tarjeta
 */
public interface ActivateCardUseCase {

    /**
     * Activa una tarjeta emitida, verificando el PIN y el límite de crédito
     * ISSUED → ACTIVE
     */
    StateTransition execute(ActivateCardCommand command);

    record ActivateCardCommand(
            CardId cardId,
            PIN pin,
            String reason,
            String activatedBy
    ) {}
}
