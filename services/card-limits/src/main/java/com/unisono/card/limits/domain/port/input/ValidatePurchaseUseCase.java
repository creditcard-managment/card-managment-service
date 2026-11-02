package com.unisono.card.limits.domain.port.input;

import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;

/**
 * Use Case: Validar si una compra o adelanto puede realizarse
 */
public interface ValidatePurchaseUseCase {

    /**
     * Valida si una compra puede ser realizada
     */
    ValidationResult validatePurchase(ValidatePurchaseCommand command);

    /**
     * Valida si un adelanto puede ser realizado
     */
    ValidationResult validateCashAdvance(ValidateCashAdvanceCommand command);

    // ========================================
    // Commands
    // ========================================

    record ValidatePurchaseCommand(
            CardId cardId,
            Money purchaseAmount
    ) {}

    record ValidateCashAdvanceCommand(
            CardId cardId,
            Money advanceAmount
    ) {}

    // ========================================
    // Result
    // ========================================

    record ValidationResult(
            boolean approved,
            String reason,
            Money availableCredit,
            Money requestedAmount
    ) {
        public static ValidationResult approved(Money availableCredit, Money requestedAmount) {
            return new ValidationResult(
                    true,
                    "Transacción aprobada",
                    availableCredit,
                    requestedAmount
            );
        }

        public static ValidationResult rejected(String reason, Money availableCredit, Money requestedAmount) {
            return new ValidationResult(
                    false,
                    reason,
                    availableCredit,
                    requestedAmount
            );
        }
    }
}
