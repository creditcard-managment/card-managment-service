package com.unisono.card.payment.application.service;

import com.unisono.card.payment.application.dto.request.ProcessRefundCommand;
import com.unisono.card.payment.application.mapper.RefundMapper;
import com.unisono.card.payment.domain.exception.TransactionNotFoundException;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.port.input.ProcessRefundUseCase;
import com.unisono.card.payment.domain.port.output.PaymentEventPublisher;
import com.unisono.card.payment.domain.port.output.TransactionRepository;
import com.unisono.card.payment.domain.service.PaymentDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for processing refunds
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundApplicationService implements ProcessRefundUseCase {

    private final PaymentDomainService paymentDomainService;
    private final TransactionRepository transactionRepository;
    private final PaymentEventPublisher eventPublisher;
    private final RefundMapper refundMapper;

    /**
     * Process a full refund: refunds everything not yet refunded of the original transaction
     */
    @Override
    @Transactional
    public Transaction processFullRefund(ProcessRefundCommand command) {
        return refund(command, true);
    }

    /**
     * Process a partial refund: the amount cannot exceed what is not yet refunded
     */
    @Override
    @Transactional
    public Transaction processPartialRefund(ProcessRefundCommand command) {
        return refund(command, false);
    }

    private Transaction refund(ProcessRefundCommand command, boolean fullRefund) {
        log.info("Processing {} refund for transaction: {}, amount: {}",
                fullRefund ? "full" : "partial", command.getOriginalTransactionId(), command.getRefundAmount());

        try {
            // 1. Map command to domain components
            RefundMapper.RefundComponents components = refundMapper.toRefundComponents(command);

            // 2. Load original transaction locking it: refunds of the same transaction are serialized
            Transaction originalTransaction = transactionRepository.findByIdForUpdate(components.getOriginalTransactionId())
                    .orElseThrow(() -> new TransactionNotFoundException(components.getOriginalTransactionId()));

            // 3. Previous refunds, to control the refunded total
            List<Transaction> previousRefunds = transactionRepository.findRefundsOf(originalTransaction.getTransactionId());

            // 4. Process refund through domain service
            Transaction refundTransaction = paymentDomainService.processRefund(
                    originalTransaction,
                    components.getCardId(),
                    components.getAmount(),
                    previousRefunds,
                    fullRefund
            );

            // 5. Persist refund transaction
            Transaction savedRefund = transactionRepository.save(refundTransaction);

            // 6. Publish domain events
            eventPublisher.publishAll(savedRefund.getDomainEvents());
            savedRefund.clearDomainEvents();

            log.info("Refund processed successfully: {}", savedRefund.getTransactionId());
            return savedRefund;

        } catch (Exception ex) {
            log.error("Error processing refund for transaction: {}", command.getOriginalTransactionId(), ex);
            throw ex;
        }
    }
}
