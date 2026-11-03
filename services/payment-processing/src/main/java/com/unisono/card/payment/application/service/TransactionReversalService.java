package com.unisono.card.payment.application.service;

import com.unisono.card.payment.domain.exception.InvalidTransactionException;
import com.unisono.card.payment.domain.exception.TransactionNotFoundException;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.port.input.ReverseTransactionUseCase;
import com.unisono.card.payment.domain.port.output.PaymentEventPublisher;
import com.unisono.card.payment.domain.port.output.TransactionRepository;
import com.unisono.card.payment.domain.service.PaymentDomainService;
import com.unisono.card.payment.domain.service.TransactionValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for reversing transactions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionReversalService implements ReverseTransactionUseCase {

    private final PaymentDomainService paymentDomainService;
    private final TransactionValidationService validationService;
    private final TransactionRepository transactionRepository;
    private final PaymentEventPublisher eventPublisher;

    /**
     * Reverse a transaction
     */
    @Override
    @Transactional
    public Transaction reverseTransaction(TransactionId transactionId, String reason) {
        log.info("Reversing transaction: {}, reason: {}", transactionId, reason);

        try {
            // 1. Load transaction locking it (serialized with refunds of the same transaction)
            Transaction transaction = transactionRepository.findByIdForUpdate(transactionId)
                    .orElseThrow(() -> new TransactionNotFoundException(transactionId));

            // 2. Validate transaction can be reversed. With refunds it cannot: reversing would give back
            //    the full amount again; what is left is refunded instead
            validationService.validateCanReverse(transaction);
            if (!transactionRepository.findRefundsOf(transactionId).isEmpty()) {
                throw new InvalidTransactionException(transactionId,
                        "Transaction has refunds and cannot be reversed; refund the remaining amount instead");
            }

            // 3. Reverse through domain service
            paymentDomainService.reverseTransaction(transaction, reason);

            // 4. Persist reversed transaction
            Transaction savedTransaction = transactionRepository.save(transaction);

            // 5. Publish domain events
            eventPublisher.publishAll(savedTransaction.getDomainEvents());
            savedTransaction.clearDomainEvents();

            log.info("Transaction reversed successfully: {}", transactionId);
            return savedTransaction;

        } catch (Exception ex) {
            log.error("Error reversing transaction: {}", transactionId, ex);
            throw ex;
        }
    }

    /**
     * Auto-reverse failed transaction
     */
    @Override
    @Transactional
    public Transaction autoReverse(TransactionId transactionId) {
        log.info("Auto-reversing transaction: {}", transactionId);

        return reverseTransaction(transactionId, "Automatic reversal due to timeout or system error");
    }
}
