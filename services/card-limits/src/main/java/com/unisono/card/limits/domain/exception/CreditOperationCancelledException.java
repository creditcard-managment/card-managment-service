package com.unisono.card.limits.domain.exception;

import com.unisono.card.shared.domain.exception.BusinessRuleException;

/**
 * La operación ya fue cancelada por el cliente y no se puede aplicar (HTTP 409)
 */
public class CreditOperationCancelledException extends BusinessRuleException {

    public CreditOperationCancelledException(String operationId) {
        super(String.format("La operación %s fue cancelada y no se puede aplicar", operationId));
    }
}
