package com.unisono.card.payment.application.service;

import com.unisono.card.payment.application.dto.request.ProcessCashAdvanceCommand;
import com.unisono.card.payment.application.dto.request.ProcessPaymentCommand;
import com.unisono.card.payment.application.mapper.PaymentMapper;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.model.TransactionType;
import com.unisono.card.payment.domain.port.input.ProcessPaymentUseCase;
import com.unisono.card.payment.domain.port.output.PaymentEventPublisher;
import com.unisono.card.payment.domain.port.output.TransactionRepository;
import com.unisono.card.payment.domain.service.PaymentDomainService;
import com.unisono.card.payment.domain.service.TransactionValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for processing payments
 * Orchestrates the payment processing flow
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApplicationService implements ProcessPaymentUseCase {

    private final PaymentDomainService paymentDomainService;
    private final TransactionValidationService validationService;
    private final TransactionRepository transactionRepository;
    private final PaymentEventPublisher eventPublisher;
    private final PaymentMapper paymentMapper;

    /**
     * Process a purchase transaction
     */
    @Override
    @Transactional
    public Transaction processPurchase(ProcessPaymentCommand command) {
        return process(command, TransactionType.PURCHASE);
    }

    private Transaction process(ProcessPaymentCommand command, TransactionType type) {
        log.info("Processing {} transaction for card: {}, amount: {} {}",
                type, command.getCardId(), command.getAmount(), command.getCurrency());

        try {
            // 1. Map command to domain objects
            PaymentMapper.TransactionComponents components = paymentMapper.toTransactionComponents(command);

            // 2. Validate amount and payment method
            validationService.validateAmount(components.getAmount());
            validationService.validatePaymentMethod(components.getPaymentMethod(), type);

            // 3. Process payment through domain service
            Transaction transaction = paymentDomainService.processPayment(
                    components.getCardId(),
                    components.getCardData(),
                    components.getMerchantInfo(),
                    components.getAmount(),
                    components.getPaymentMethod(),
                    type
            );

            // 4. Set terminal information if provided
            if (command.getTerminalId() != null) {
                transaction.setTerminalInfo(command.getTerminalId(), command.getPosEntryMode());
            }

            // 5. Persist transaction
            Transaction savedTransaction = transactionRepository.save(transaction);

            // 6. Publish domain events
            eventPublisher.publishAll(savedTransaction.getDomainEvents()); // ✅ Ahora es List<DomainEvent>
            savedTransaction.clearDomainEvents();

            log.info("{} transaction processed successfully: {}", type, savedTransaction.getTransactionId());
            return savedTransaction;

        } catch (Exception ex) {
            log.error("Error processing {} transaction for card: {}", type, command.getCardId(), ex);
            throw ex;
        }
    }

    /**
     * Process a cash advance transaction
     */
    @Override
    @Transactional
    public Transaction processCashAdvance(ProcessCashAdvanceCommand command) {
        log.info("Processing cash advance for card: {}, amount: {} {}",
                command.getCardId(), command.getAmount(), command.getCurrency());

        try {
            // El cajero es una terminal de payment_terminals; su comercio es el operador del ATM
            String merchantId = paymentDomainService.resolveTerminalMerchant(command.getTerminalId());

            // Map to ProcessPaymentCommand (reuse logic)
            ProcessPaymentCommand paymentCommand = ProcessPaymentCommand.builder()
                    .cardId(command.getCardId())
                    .cardNumberToken(command.getCardNumberToken())
                    .lastFourDigits(command.getLastFourDigits())
                    .expirationDate(command.getExpirationDate())
                    .amount(command.getAmount())
                    .currency(command.getCurrency())
                    .merchantId(merchantId)
                    .merchantName("ATM Cash Advance")
                    .merchantCity(command.getAtmLocation())
                    .merchantCountry(command.getCountry())
                    .paymentMethod(com.unisono.card.payment.domain.model.PaymentMethod.CHIP)
                    .terminalId(command.getTerminalId())
                    .build();

            Transaction transaction = process(paymentCommand, TransactionType.CASH_ADVANCE);

            log.info("Cash advance processed successfully: {}", transaction.getTransactionId());
            return transaction;

        } catch (Exception ex) {
            log.error("Error processing cash advance for card: {}", command.getCardId(), ex);
            throw ex;
        }
    }

    /**
     * Authorize a transaction (reserve funds without capturing)
     */
    @Override
    @Transactional
    public Transaction authorizeTransaction(TransactionId transactionId) {
        log.info("Authorizing transaction: {}", transactionId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new com.unisono.card.payment.domain.exception.TransactionNotFoundException(transactionId));

        // Transaction should already be authorized during processing
        // This method is for pre-authorization flows

        Transaction savedTransaction = transactionRepository.save(transaction);
        eventPublisher.publishAll(savedTransaction.getDomainEvents());
        savedTransaction.clearDomainEvents();

        log.info("Transaction authorized: {}", transactionId);
        return savedTransaction;
    }

    /**
     * Capture a pre-authorized transaction
     */
    @Override
    @Transactional
    public Transaction captureTransaction(TransactionId transactionId) {
        log.info("Capturing transaction: {}", transactionId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new com.unisono.card.payment.domain.exception.TransactionNotFoundException(transactionId));

        if (transaction.getStatus() != com.unisono.card.payment.domain.model.TransactionStatus.AUTHORIZED) {
            throw new com.unisono.card.payment.domain.exception.InvalidTransactionException(
                    transactionId,
                    "Only authorized transactions can be captured"
            );
        }

        // Approve the transaction (capture)
        transaction.approve();

        Transaction savedTransaction = transactionRepository.save(transaction);
        eventPublisher.publishAll(savedTransaction.getDomainEvents());
        savedTransaction.clearDomainEvents();

        log.info("Transaction captured: {}", transactionId);
        return savedTransaction;
    }
}
