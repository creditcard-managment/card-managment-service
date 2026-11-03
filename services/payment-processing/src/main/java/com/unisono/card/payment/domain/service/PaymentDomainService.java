package com.unisono.card.payment.domain.service;

import com.unisono.card.payment.domain.exception.*;
import com.unisono.card.payment.domain.model.*;
import com.unisono.card.payment.domain.port.output.*;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Domain service for payment processing business logic
 */
@Slf4j
@RequiredArgsConstructor
public class PaymentDomainService {

    private final CardValidationPort cardValidationPort;
    private final CreditLimitPort creditLimitPort;
    private final MerchantValidationPort merchantValidationPort;
    private final FraudCheckPort fraudCheckPort;
    private final AuthorizationPort authorizationPort;

    /**
     * Validate and process a debit transaction (PURCHASE or CASH_ADVANCE)
     */
    public Transaction processPayment(CardId cardId, CardData cardData, MerchantInfo merchantInfo,
                                      TransactionAmount amount, PaymentMethod paymentMethod,
                                      TransactionType type) {

        log.info("Processing {} for card: {}, merchant: {}, amount: {}",
                type, cardId, merchantInfo.getMerchantId(), amount);

        // 1. Validate card
        validateCard(cardId, cardData);

        // 2. Validate merchant
        validateMerchant(merchantInfo);

        // 3. Check available credit
        checkAvailableCredit(cardId, amount.getEffectiveAmount(), type);

        // 4. Create transaction
        Transaction transaction = type == TransactionType.CASH_ADVANCE
                ? Transaction.createCashAdvance(cardId, cardData, merchantInfo, amount, paymentMethod)
                : Transaction.createPurchase(cardId, cardData, merchantInfo, amount, paymentMethod);

        // 5. Fraud check
        performFraudCheck(transaction);

        // 6. Request authorization
        AuthorizationCode authCode = requestAuthorization(transaction);

        // 7. Authorize transaction (fails on a declined code, before touching the credit)
        Integer riskScore = fraudCheckPort.calculateRiskScore(transaction);
        transaction.authorize(authCode, riskScore);

        // 8. Consume credit in card-limits
        creditLimitPort.consumeCredit(cardId, amount.getEffectiveAmount(), type,
                creditDescription(transaction));

        // 9. Approve transaction
        transaction.approve();

        log.info("Payment processed successfully: {}", transaction.getTransactionId());
        return transaction;
    }

    /**
     * Process a refund transaction
     */
    public Transaction processRefund(CardId cardId, CardData cardData, MerchantInfo merchantInfo,
                                     TransactionAmount amount, TransactionId originalTransactionId) {

        log.info("Processing refund for original transaction: {}, amount: {}",
                originalTransactionId, amount);

        // 1. Validate card
        validateCard(cardId, cardData);

        // 2. Create refund transaction
        Transaction refundTransaction = Transaction.createRefund(
                cardId, cardData, merchantInfo, amount, originalTransactionId
        );

        // 3. Auto-approve refunds (no authorization needed)
        refundTransaction.approveRefund(originalTransactionId);

        // 4. Give the credit back to the card
        creditLimitPort.releaseCredit(cardId, amount.getEffectiveAmount(),
                "Refund " + refundTransaction.getTransactionId() + " of " + originalTransactionId);

        log.info("Refund processed successfully: {}", refundTransaction.getTransactionId());
        return refundTransaction;
    }

    /**
     * Reverse a transaction
     */
    public void reverseTransaction(Transaction transaction, String reason) {
        log.info("Reversing transaction: {}, reason: {}", transaction.getTransactionId(), reason);

        // 1. Reverse the transaction
        transaction.reverse(reason);

        // 2. Give the consumed credit back
        Money amount = transaction.getAmount().getEffectiveAmount();
        creditLimitPort.releaseCredit(transaction.getCardId(), amount,
                "Reversal of " + transaction.getTransactionId() + ": " + reason);

        log.info("Transaction reversed successfully: {}", transaction.getTransactionId());
    }

    /**
     * Settle a transaction
     */
    public void settleTransaction(Transaction transaction) {
        log.info("Settling transaction: {}", transaction.getTransactionId());

        if (!transaction.requiresSettlement()) {
            throw new InvalidTransactionException(
                    transaction.getTransactionId(),
                    "Transaction does not require settlement"
            );
        }

        // Settle the transaction. The credit was already consumed when it was approved
        transaction.settle();

        log.info("Transaction settled successfully: {}", transaction.getTransactionId());
    }

    /**
     * Merchant that owns the terminal (ATM) used for a cash advance
     */
    public String resolveTerminalMerchant(String terminalId) {
        return merchantValidationPort.findMerchantIdByTerminal(terminalId)
                .orElseThrow(() -> MerchantValidationException.unknownTerminal(terminalId));
    }

    // ==================== Private validation methods ====================

    private void validateCard(CardId cardId, CardData cardData) {
        CardValidationPort.CardStatusSnapshot card = cardValidationPort.findCardStatus(cardId)
                .orElseThrow(() -> CardValidationException.notFound(cardId));

        if (card.blocked()) {
            throw CardValidationException.blocked(cardId);
        }

        if (!card.isActive()) {
            throw CardValidationException.invalidStatus(cardId, card.status());
        }

        // Vencimiento según card-data y según la fecha que envía el cliente
        if (card.expired() || cardData.isExpired()) {
            throw CardValidationException.expired(cardId);
        }
    }

    private void validateMerchant(MerchantInfo merchantInfo) {
        if (!merchantValidationPort.isMerchantActive(merchantInfo.getMerchantId())) {
            throw MerchantValidationException.inactive(merchantInfo.getMerchantId());
        }

        if (merchantInfo.getMerchantCategoryCode() != null
                && !merchantValidationPort.isValidMCC(merchantInfo.getMerchantCategoryCode())) {
            throw new MerchantValidationException(
                    merchantInfo.getMerchantId(),
                    "MCC",
                    "Invalid merchant category code: " + merchantInfo.getMerchantCategoryCode()
            );
        }
    }

    private void checkAvailableCredit(CardId cardId, Money requestedAmount, TransactionType type) {
        CreditLimitPort.CreditCheck check = creditLimitPort.checkCredit(cardId, requestedAmount, type);
        if (!check.approved()) {
            throw new InsufficientFundsException(cardId, requestedAmount, check.available(), check.reason());
        }
    }

    private static String creditDescription(Transaction transaction) {
        MerchantInfo merchant = transaction.getMerchantInfo();
        String merchantName = merchant.getMerchantName() != null ? merchant.getMerchantName() : merchant.getMerchantId();
        return String.format("%s %s - %s", transaction.getType().getDisplayName(),
                transaction.getTransactionId(), merchantName);
    }

    private void performFraudCheck(Transaction transaction) {
        if (fraudCheckPort.isSuspicious(transaction)) {
            Integer riskScore = fraudCheckPort.calculateRiskScore(transaction);
            throw FraudDetectedException.highRiskScore(
                    transaction.getTransactionId(),
                    transaction.getCardId(),
                    riskScore
            );
        }

        if (fraudCheckPort.violatesVelocityRules(transaction.getCardId())) {
            throw FraudDetectedException.velocityViolation(
                    transaction.getTransactionId(),
                    transaction.getCardId()
            );
        }
    }

    private AuthorizationCode requestAuthorization(Transaction transaction) {
        try {
            return authorizationPort.requestAuthorization(transaction);
        } catch (Exception e) {
            log.error("Authorization failed for transaction: {}", transaction.getTransactionId(), e);
            throw AuthorizationFailedException.cardRestricted(transaction.getTransactionId());
        }
    }
}
