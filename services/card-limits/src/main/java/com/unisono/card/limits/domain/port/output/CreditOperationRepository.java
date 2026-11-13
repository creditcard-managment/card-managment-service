package com.unisono.card.limits.domain.port.output;

import com.unisono.card.limits.domain.model.CreditOperation;

import java.util.Optional;

/**
 * Port de salida: operaciones de crédito identificadas por el cliente
 */
public interface CreditOperationRepository {

    Optional<CreditOperation> findById(String operationId);

    CreditOperation save(CreditOperation operation);
}
