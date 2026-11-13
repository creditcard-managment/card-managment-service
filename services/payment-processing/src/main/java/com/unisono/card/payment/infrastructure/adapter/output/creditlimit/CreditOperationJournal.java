package com.unisono.card.payment.infrastructure.adapter.output.creditlimit;

import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.PaymentCreditOperationEntity;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.PaymentCreditOperationEntity.Status;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.PaymentCreditOperationEntity.Type;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.repository.JpaPaymentCreditOperationRepository;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de las operaciones de crédito pedidas a card-limits (tabla payment_credit_operations)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditOperationJournal {

    private final JpaPaymentCreditOperationRepository repository;

    /**
     * Registra la operación como PENDING en una transacción propia, que confirma antes de llamar a card-limits:
     * así queda registrada aunque la transacción del pago no confirme nunca.
     *
     * @return el id de la operación (lo recibe card-limits)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String begin(TransactionId transactionId, CardId cardId, Type type, Money amount) {
        LocalDateTime now = LocalDateTime.now();
        PaymentCreditOperationEntity operation = PaymentCreditOperationEntity.builder()
                .operationId(UUID.randomUUID().toString())
                .transactionId(transactionId.getValue())
                .cardId(cardId.getValue())
                .operationType(type)
                .amount(amount.getAmount())
                .currency(amount.getCurrencyCode())
                .status(Status.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();
        repository.save(operation);
        log.debug("Credit operation {} ({}) registered for transaction {}", operation.getOperationId(), type, transactionId);
        return operation.getOperationId();
    }

    /**
     * Toma la operación en la transacción del pago, hasta que confirme o se deshaga
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(String operationId) {
        repository.lockById(operationId)
                .orElseThrow(() -> new IllegalStateException("Credit operation not found: " + operationId));
    }

    /**
     * card-limits la aplicó. Se confirma junto con el pago: si el pago no confirma, vuelve a PENDING
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void confirm(String operationId) {
        resolve(operationId, Status.CONFIRMED);
    }

    /**
     * card-limits la rechazó (por ejemplo, crédito insuficiente): no aplicó nada
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reject(String operationId) {
        resolve(operationId, Status.REJECTED);
    }

    /**
     * Se canceló en card-limits fuera de la transacción del pago (después de que se deshizo)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompensated(String operationId) {
        repository.findById(operationId).ifPresent(operation -> {
            if (operation.getStatus() == Status.PENDING) {
                operation.markCompensated();
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompensationFailed(String operationId, String error) {
        repository.findById(operationId).ifPresent(operation -> {
            if (operation.getStatus() == Status.PENDING) {
                operation.markCompensationFailed(error);
            }
        });
    }

    private void resolve(String operationId, Status status) {
        if (repository.resolvePending(operationId, status, LocalDateTime.now()) != 1) {
            throw new IllegalStateException("Credit operation " + operationId + " is not pending");
        }
    }
}
