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

    /**
     * Deshace el consumo de una transacción reversada o devuelta (disponible, cupo diario y mensual)
     */
    CreditLimit restoreCredit(RestoreCreditCommand command);

    /**
     * Deshace una operación de consumo o restauración identificada por el cliente.
     * Si la operación todavía no llegó, la anula: cuando llegue se rechaza. Es idempotente.
     */
    CancelOperationResult cancelOperation(CancelOperationCommand command);

    // ========================================
    // Commands
    // ========================================

    /**
     * @param operationId opcional: con un id, la operación es idempotente y se puede cancelar
     */
    record ConsumeCreditCommand(
            String creditLimitId,
            Money amount,
            String description,
            String operationId
    ) {
        public ConsumeCreditCommand(String creditLimitId, Money amount, String description) {
            this(creditLimitId, amount, description, null);
        }
    }

    /**
     * @param operationId opcional: con un id, la operación es idempotente y se puede cancelar
     */
    record RestoreCreditCommand(
            String creditLimitId,
            Money amount,
            java.time.LocalDate originalTransactionDate,
            String description,
            String operationId
    ) {
        public RestoreCreditCommand(String creditLimitId, Money amount, java.time.LocalDate originalTransactionDate,
                                    String description) {
            this(creditLimitId, amount, originalTransactionDate, description, null);
        }
    }

    record CancelOperationCommand(
            String creditLimitId,
            String operationId
    ) {}

    /**
     * @param outcome     qué pasó con la operación
     * @param creditLimit el límite después de la cancelación; {@link CreditLimit#getOverLimitAmount()} es mayor
     *                    a cero si deshacer una restauración dejó la tarjeta excedida
     */
    record CancelOperationResult(
            CancelOutcome outcome,
            CreditLimit creditLimit
    ) {}

    enum CancelOutcome {
        /** Estaba aplicada y se deshizo */
        CANCELLED,
        /** No había llegado: queda anulada y se rechaza si llega después */
        VOIDED,
        /** Ya estaba cancelada o anulada: no se hizo nada */
        ALREADY_CANCELLED
    }

    record ReleaseCreditCommand(
            String creditLimitId,
            Money paymentAmount,
            String description
    ) {}
}
