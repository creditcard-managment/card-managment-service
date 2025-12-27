package com.unisono.card.limits.domain.port.input;

import com.unisono.card.limits.domain.model.CreditLimit;
import com.unisono.card.shared.domain.valueobject.Money;

/**
 * Use Case: Recalcular crédito disponible
 *
 * Este use case se ejecuta después de transacciones o pagos.
 */
public interface RecalculateAvailableCreditUseCase {

    /**
     * Consume crédito por una compra
     */
    CreditLimit consumeForPurchase(ConsumeCreditCommand command);

    /**
     * Consume crédito por un adelanto
     */
    CreditLimit consumeForCashAdvance(ConsumeCreditCommand command);

    /**
     * Libera crédito después de un pago
     */
    CreditLimit releaseCredit(ReleaseCreditCommand command);

    // ========================================
    // Commands
    // ========================================

    record ConsumeCreditCommand(
            String creditLimitId,
            Money amount,
            String description
    ) {}

    record ReleaseCreditCommand(
            String creditLimitId,
            Money paymentAmount,
            String description
    ) {}
}
