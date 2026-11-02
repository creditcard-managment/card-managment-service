package com.unisono.card.limits.domain.port.output;

import com.unisono.card.shared.domain.valueobject.CardId;

/**
 * Port de salida: Validación de tarjetas
 *
 * Permite validar que la tarjeta existe y está activa
 * antes de asignar límites.
 */
public interface CardValidationPort {

    /**
     * Verifica si la tarjeta existe
     */
    boolean cardExists(CardId cardId);

    /**
     * Verifica si la tarjeta está activa
     */
    boolean isCardActive(CardId cardId);

    /**
     * Verifica si la tarjeta puede recibir límite de crédito
     */
    boolean canAssignCreditLimit(CardId cardId);
}
