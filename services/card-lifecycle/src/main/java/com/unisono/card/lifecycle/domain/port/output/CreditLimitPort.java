package com.unisono.card.lifecycle.domain.port.output;

import com.unisono.card.shared.domain.valueobject.CardId;

/**
 * Puerto de salida para validar límites de crédito
 */
public interface CreditLimitPort {

    /**
     * Verifica si la tarjeta tiene límite asignado
     */
    boolean hasLimitAssigned(CardId cardId);

    /**
     * Verifica si la tarjeta puede ser activada
     * (debe tener límite asignado)
     */
    boolean canActivate(CardId cardId);
}
