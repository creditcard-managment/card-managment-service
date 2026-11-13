package com.unisono.card.payment.infrastructure.adapter.output.creditlimit;

import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Cancela en card-limits una operación cuyo pago no confirmó, apenas se deshace la transacción.
 * Si falla, la operación queda PENDING y la reintenta {@link CreditOperationReconciler}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditOperationCompensator {

    private final CreditLimitAdapter creditLimitAdapter;
    private final CreditOperationJournal journal;

    public void compensate(String operationId, CardId cardId) {
        try {
            String outcome = creditLimitAdapter.cancelOperation(operationId, cardId);
            journal.markCompensated(operationId);
            log.warn("↩️ Credit operation {} of card {} compensated after its payment transaction rolled back: {}",
                    operationId, cardId, outcome);
        } catch (Exception ex) {
            log.error("Could not compensate credit operation {} of card {}, it will be retried: {}",
                    operationId, cardId, ex.getMessage());
            try {
                journal.markCompensationFailed(operationId, ex.getMessage());
            } catch (Exception journalError) {
                log.error("Could not record the failed compensation of credit operation {}", operationId, journalError);
            }
        }
    }
}
