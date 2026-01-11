package com.unisono.card.payment.application.service;

import com.unisono.card.payment.application.dto.request.ProcessRefundCommand;
import com.unisono.card.payment.application.mapper.RefundMapper;
import com.unisono.card.payment.domain.exception.InvalidTransactionException;
import com.unisono.card.payment.domain.exception.TransactionNotFoundException;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionStatus;
import com.unisono.card.payment.domain.port.input.ProcessRefundUseCase;
import com.unisono.card.payment.domain.port.output.PaymentEventPublisher;
import com.unisono.card.payment.domain.port.output.TransactionRepository;
import com.unisono.card.payment.domain.service.PaymentDomainService;
import com.unisono.card.payment.domain.service.TransactionValidationService;
import com.unisono.card.shared.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for processing refunds
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundApplicationService implements ProcessRefundUseCase {

    private final PaymentDomainService paymentDomainService;
    private final TransactionValidationService validationService;
    private final TransactionRepository transactionRepository;
    private final PaymentEventPublisher eventPublisher;
    private final RefundMapper refundMapper;

    /**
     * Process a full refund
     */
    @Override
    @Transactional
    public Transaction processFullRefund(ProcessRefundCommand command) {
        log.info("Processing full refund for transaction: {}", command.getOriginalTransactionId());

        try {
            // 1. Map command to domain components
            RefundMapper.RefundComponents components = refundMapper.toRefundComponents(command);

            // 2. Load original transaction
            Transaction originalTransaction = transactionRepository.findById(components.getOriginalTransactionId())
                    .orElseThrow(() -> new TransactionNotFoundException(components.getOriginalTransactionId()));

            // 3. Validate original transaction can be refunded
            validateCanRefund(originalTransaction);

            // 4. Validate refund amount equals original amount for full refund
            Money originalAmount = originalTransaction.getAmount().getEffectiveAmount();
            Money refundAmount = components.getAmount().getEffectiveAmount();

            if (refundAmount.getAmount().compareTo(originalAmount.getAmount()) != 0) {
                throw new InvalidTransactionException(
                        "Full refund amount must match original transaction amount"
                );
            }

            // 5. Process refund through domain service
            Transaction refundTransaction = paymentDomainService.processRefund(
                    components.getCardId(),
                    originalTransaction.getCardData(),
                    originalTransaction.getMerchantInfo(),
                    components.getAmount(),
                    components.getOriginalTransactionId()
            );

            // 6. Persist refund transaction
            Transaction savedRefund = transactionRepository.save(refundTransaction);

            // 7. Publish domain events
            eventPublisher.publishAll(savedRefund.getDomainEvents());
            savedRefund.clearDomainEvents();

            log.info("Full refund processed successfully: {}", savedRefund.getTransactionId());
            return savedRefund;

        } catch (Exception ex) {
            log.error("Error processing full refund for transaction: {}", command.getOriginalTransactionId(), ex);
            throw ex;
        }
    }

    /**
     * Process a partial refund
     */
    @Override
    @Transactional
    public Transaction processPartialRefund(ProcessRefundCommand command) {
        log.info("Processing partial refund for transaction: {}, amount: {}",
                command.getOriginalTransactionId(), command.getRefundAmount());

        try {
            // 1. Map command to domain components
            RefundMapper.RefundComponents components = refundMapper.toRefundComponents(command);

            // 2. Load original transaction
            Transaction originalTransaction = transactionRepository.findById(components.getOriginalTransactionId())
                    .orElseThrow(() -> new TransactionNotFoundException(components.getOriginalTransactionId()));

            // 3. Validate original transaction can be refunded
            validateCanRefund(originalTransaction);

            // 4. Validate partial refund amount is less than original
            Money originalAmount = originalTransaction.getAmount().getEffectiveAmount();
            Money refundAmount = components.getAmount().getEffectiveAmount();

            validationService.validateRefundAmount(refundAmount, originalAmount);

            // 5. Process refund through domain service
            Transaction refundTransaction = paymentDomainService.processRefund(
                    components.getCardId(),
                    originalTransaction.getCardData(),
                    originalTransaction.getMerchantInfo(),
                    components.getAmount(),
                    components.getOriginalTransactionId()
            );

            // 6. Persist refund transaction
            Transaction savedRefund = transactionRepository.save(refundTransaction);

            // 7. Publish domain events
            eventPublisher.publishAll(savedRefund.getDomainEvents());
            savedRefund.clearDomainEvents();

            log.info("Partial refund processed successfully: {}", savedRefund.getTransactionId());
            return savedRefund;

        } catch (Exception ex) {
            log.error("Error processing partial refund for transaction: {}", command.getOriginalTransactionId(), ex);
            throw ex;
        }
    }

    private void validateCanRefund(Transaction originalTransaction) {
        if (originalTransaction.getStatus() != TransactionStatus.APPROVED
                && originalTransaction.getStatus() != TransactionStatus.SETTLED) {
            throw new InvalidTransactionException(
                    originalTransaction.getTransactionId(),
                    "Only approved or settled transactions can be refunded"
            );
        }
    }
}
