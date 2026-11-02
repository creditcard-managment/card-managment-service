package com.unisono.card.limits.domain.port.input;

import com.unisono.card.limits.domain.model.CreditLimit;
import com.unisono.card.limits.domain.model.CreditLimitHistory;
import com.unisono.card.shared.domain.valueobject.CardId;

import java.util.List;
import java.util.Optional;

/**
 * Use Case: Consultar información de límites de crédito
 */
public interface GetCreditInfoUseCase {

    /**
     * Obtiene el límite de crédito por ID
     */
    Optional<CreditLimit> findById(String creditLimitId);

    /**
     * Obtiene el límite de crédito de una tarjeta
     */
    Optional<CreditLimit> findByCardId(CardId cardId);

    /**
     * Obtiene el historial de cambios de un límite
     */
    List<CreditLimitHistory> getHistory(String creditLimitId);

    /**
     * Verifica si una tarjeta tiene límite asignado
     */
    boolean hasLimitAssigned(CardId cardId);
}
