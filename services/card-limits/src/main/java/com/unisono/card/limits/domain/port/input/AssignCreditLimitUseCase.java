package com.unisono.card.limits.domain.port.input;

import com.unisono.card.limits.domain.model.CreditLimit;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;

/**
 * Use Case: Asignar límite de crédito a una tarjeta
 */
public interface AssignCreditLimitUseCase {

    /**
     * Asigna un nuevo límite de crédito a una tarjeta
     *
     * @param command datos para asignar el límite
     * @return el límite de crédito creado
     */
    CreditLimit execute(AssignCreditLimitCommand command);

    /**
     * Command para asignar límite
     */
    record AssignCreditLimitCommand(
            CardId cardId,
            Money purchaseLimitAmount,
            double cashAdvancePercentage,
            Money dailyLimitAmount,
            Money monthlyLimitAmount,
            String assignedBy
    ) {
        public AssignCreditLimitCommand {
            // Valores por defecto si no se especifican
            if (cashAdvancePercentage == 0) {
                cashAdvancePercentage = 0.30; // 30% por defecto
            }
        }
    }
}
