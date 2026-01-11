package com.unisono.card.payment.application.service;

import com.unisono.card.payment.domain.exception.TransactionNotFoundException;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.port.input.SettleTransactionsUseCase;
import com.unisono.card.payment.domain.port.output.PaymentEventPublisher;
import com.unisono.card.payment.domain.port.output.TransactionRepository;
import com.unisono.card.payment.domain.service.PaymentDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Application service for settling transactions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService implements SettleTransactionsUseCase {

    private final PaymentDomainService paymentDomainService;
    private final TransactionRepository transactionRepository;
    private final PaymentEventPublisher eventPublisher;

    /**
     * Settle a single transaction
     */
    @Override
    @Transactional
    public Transaction settleTransaction(TransactionId transactionId) {
        log.info("Settling transaction: {}", transactionId);

        try {
            // 1. Load transaction
            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new TransactionNotFoundException(transactionId));

            // 2. Settle through domain service
            paymentDomainService.settleTransaction(transaction);

            // 3. Persist settled transaction
            Transaction savedTransaction = transactionRepository.save(transaction);

            // 4. Publish domain events
            eventPublisher.publishAll(savedTransaction.getDomainEvents());
            savedTransaction.clearDomainEvents();

            log.info("Transaction settled successfully: {}", transactionId);
            return savedTransaction;

        } catch (Exception ex) {
            log.error("Error settling transaction: {}", transactionId, ex);
            throw ex;
        }
    }

    /**
     * Settle multiple transactions in batch
     */
    @Override
    @Transactional
    public List<Transaction> settleBatch(List<TransactionId> transactionIds) {
        log.info("Settling batch of {} transactions", transactionIds.size());

        List<Transaction> settledTransactions = new ArrayList<>();

        for (TransactionId transactionId : transactionIds) {
            try {
                Transaction settled = settleTransaction(transactionId);
                settledTransactions.add(settled);
            } catch (Exception ex) {
                log.error("Failed to settle transaction {} in batch", transactionId, ex);
                // Continue with other transactions
            }
        }

        log.info("Batch settlement completed: {}/{} transactions settled",
                settledTransactions.size(), transactionIds.size());

        return settledTransactions;
    }

    /**
     * Settle all pending transactions
     */
    @Override
    @Transactional
    public List<Transaction> settleAllPending() {
        log.info("Settling all pending transactions");

        List<Transaction> pendingTransactions = transactionRepository.findPendingSettlement();
        log.info("Found {} transactions pending settlement", pendingTransactions.size());

        List<TransactionId> transactionIds = pendingTransactions.stream()
                .map(Transaction::getTransactionId)
                .toList();

        return settleBatch(transactionIds);
    }
}
