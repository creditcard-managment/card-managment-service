package com.unisono.card.lifecycle.domain.port.input;


import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.shared.domain.valueobject.*;

/**
 * Use Case: Emitir tarjeta
 */
public interface IssueCardUseCase {

    /**
     * Emite una tarjeta (asigna PAN, CVV, PIN, fecha de vencimiento)
     * APPROVED → ISSUED
     */
    StateTransition execute(IssueCardCommand command);

    record IssueCardCommand(
            CardId cardId,
            CardNumber cardNumber,
            CVV cvv,
            PIN pin,
            ExpirationDate expirationDate,
            String reason,
            String issuedBy
    ) {}
}
