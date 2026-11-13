package com.unisono.card.limits.domain.port.output;

import com.unisono.card.limits.domain.model.CreditLimit;
import com.unisono.card.limits.domain.model.CreditLimitHistory;
import com.unisono.card.shared.domain.valueobject.CardId;

import java.util.List;
import java.util.Optional;

/**
 * Port de salida: Repositorio de límites de crédito
 */
public interface CreditLimitRepository {

    /**
     * Guarda o actualiza un límite de crédito
     */
    CreditLimit save(CreditLimit creditLimit);

    /**
     * Busca un límite de crédito por ID
     */
    Optional<CreditLimit> findById(String creditLimitId);

    /**
     * Busca un límite de crédito bloqueándolo hasta el fin de la transacción
     */
    Optional<CreditLimit> findByIdForUpdate(String creditLimitId);

    /**
     * Busca el límite de crédito de una tarjeta
     */
    Optional<CreditLimit> findByCardId(CardId cardId);

    /**
     * Verifica si existe un límite para una tarjeta
     */
    boolean existsByCardId(CardId cardId);

    /**
     * Elimina un límite de crédito
     */
    void delete(String creditLimitId);

    /**
     * Guarda un registro de historial
     */
    CreditLimitHistory saveHistory(CreditLimitHistory history);

    /**
     * Obtiene el historial de un límite
     */
    List<CreditLimitHistory> findHistoryByCreditLimitId(String creditLimitId);
}
