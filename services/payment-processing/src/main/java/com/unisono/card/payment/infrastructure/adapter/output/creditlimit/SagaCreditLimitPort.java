package com.unisono.card.payment.infrastructure.adapter.output.creditlimit;

import com.unisono.card.payment.domain.exception.InsufficientFundsException;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.model.TransactionType;
import com.unisono.card.payment.domain.port.output.CreditLimitPort;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.PaymentCreditOperationEntity.Type;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;

/**
 * {@link CreditLimitPort} con compensación: cada consumo o restauración en card-limits queda atado a la
 * transacción de base de datos que guarda el pago.
 * <ol>
 *   <li>Registra la operación PENDING en una transacción propia ({@link CreditOperationJournal#begin}).</li>
 *   <li>La toma en la transacción del pago y llama a card-limits con su operationId (idempotente).</li>
 *   <li>Si card-limits la aplica, la marca CONFIRMED en la transacción del pago; si la rechaza, REJECTED.</li>
 *   <li>Si la transacción del pago no confirma (falla el guardado, el outbox, el commit o la misma llamada),
 *       la operación sigue PENDING: se cancela en card-limits apenas termina la transacción
 *       ({@link CreditOperationCompensator}) y, si eso falla o el proceso se cae, la reintenta
 *       {@link CreditOperationReconciler}.</li>
 * </ol>
 * Cancelar es seguro aunque la operación no haya llegado a card-limits: queda anulada y se rechaza si llega después.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaCreditLimitPort implements CreditLimitPort {

    private final CreditLimitAdapter creditLimitAdapter;
    private final CreditOperationJournal journal;
    private final CreditOperationCompensator compensator;

    @Override
    public CreditCheck checkCredit(CardId cardId, Money amount, TransactionType type) {
        return creditLimitAdapter.checkCredit(cardId, amount, type);
    }

    @Override
    public void consumeCredit(TransactionId transactionId, CardId cardId, Money amount, TransactionType type,
                              String description) {
        Type operationType = type == TransactionType.CASH_ADVANCE ? Type.CONSUME_CASH_ADVANCE : Type.CONSUME_PURCHASE;
        String operationId = start(transactionId, cardId, operationType, amount);
        try {
            creditLimitAdapter.consumeCredit(operationId, cardId, amount, type, description);
        } catch (InsufficientFundsException rejected) {
            // card-limits la rechazó dentro de su transacción: no aplicó nada y no hay que compensar
            journal.reject(operationId);
            throw rejected;
        }
        journal.confirm(operationId);
    }

    @Override
    public void restoreCredit(TransactionId transactionId, CardId cardId, Money amount,
                              LocalDate originalTransactionDate, String description) {
        String operationId = start(transactionId, cardId, Type.RESTORE, amount);
        creditLimitAdapter.restoreCredit(operationId, cardId, amount, originalTransactionDate, description);
        journal.confirm(operationId);
    }

    private String start(TransactionId transactionId, CardId cardId, Type type, Money amount) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Credit operations must run inside the payment's database transaction");
        }

        String operationId = journal.begin(transactionId, cardId, type, amount);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    log.warn("Payment transaction {} rolled back: compensating credit operation {} ({})",
                            transactionId, operationId, type);
                    compensator.compensate(operationId, cardId);
                } else if (status == STATUS_UNKNOWN) {
                    // No se sabe si el commit llegó: lo decide el reconciliador, que solo compensa si sigue PENDING
                    log.warn("Outcome of payment transaction {} is unknown: credit operation {} is left to the reconciler",
                            transactionId, operationId);
                }
            }
        });
        journal.lock(operationId);
        return operationId;
    }
}
