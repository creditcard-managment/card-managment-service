package com.unisono.card.limits.domain.port.input;

import com.unisono.card.limits.domain.model.CreditLimit;
import com.unisono.card.shared.domain.valueobject.Money;

/**
 * Use Case: Actualizar límites de crédito
 */
public interface UpdateCreditLimitUseCase {

    /**
     * Aumenta el límite de compras
     */
    CreditLimit increasePurchaseLimit(IncreasePurchaseLimitCommand command);

    /**
     * Disminuye el límite de compras
     */
    CreditLimit decreasePurchaseLimit(DecreasePurchaseLimitCommand command);

    /**
     * Actualiza el límite de adelantos
     */
    CreditLimit updateCashAdvanceLimit(UpdateCashAdvanceLimitCommand command);

    /**
     * Actualiza los límites temporales
     */
    CreditLimit updateTemporalLimits(UpdateTemporalLimitsCommand command);

    // ========================================
    // Commands
    // ========================================

    record IncreasePurchaseLimitCommand(
            String creditLimitId,
            Money additionalAmount,
            String reason,
            String changedBy
    ) {}

    record DecreasePurchaseLimitCommand(
            String creditLimitId,
            Money reductionAmount,
            String reason,
            String changedBy
    ) {}

    record UpdateCashAdvanceLimitCommand(
            String creditLimitId,
            Money newAmount,
            String reason,
            String changedBy
    ) {}

    record UpdateTemporalLimitsCommand(
            String creditLimitId,
            Money dailyLimitAmount,
            Money monthlyLimitAmount,
            String reason,
            String changedBy
    ) {}
}
