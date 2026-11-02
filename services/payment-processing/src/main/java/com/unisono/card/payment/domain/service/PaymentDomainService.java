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
     * Validate and process a payment transaction
     */
    public Transaction processPayment(CardId cardId, CardData cardData, MerchantInfo merchantInfo,
                                      TransactionAmount amount, PaymentMethod paymentMethod) {

        log.info("Processing payment for card: {}, merchant: {}, amount: {}",
                cardId, merchantInfo.getMerchantId(), amount);

        // 1. Validate card
        validateCard(cardId, cardData);

        // 2. Validate merchant
        validateMerchant(merchantInfo);

        // 3. Check available credit
        checkAvailableCredit(cardId, amount.getEffectiveAmount());

        // 4. Create transaction
        Transaction transaction = Transaction.createPurchase(
                cardId, cardData, merchantInfo, amount, paymentMethod
        );

        // 5. Fraud check
        performFraudCheck(transaction);

        // 6. Request authorization
        AuthorizationCode authCode = requestAuthorization(transaction);

        // 7. Reserve credit
        creditLimitPort.reserveCredit(cardId, amount.getEffectiveAmount());

        // 8. Authorize transaction
        Integer riskScore = fraudCheckPort.calculateRiskScore(transaction);
        transaction.authorize(authCode, riskScore);

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
        refundTransaction.approve();

        // 4. Restore credit to card
        creditLimitPort.restoreCredit(cardId, amount.getEffectiveAmount());

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

        // 2. Release reserved credit
        Money amount = transaction.getAmount().getEffectiveAmount();
        creditLimitPort.releaseCredit(transaction.getCardId(), amount);

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

        // 1. Settle the transaction
        transaction.settle();

        // 2. Finalize credit consumption
        Money amount = transaction.getAmount().getEffectiveAmount();
        creditLimitPort.consumeCredit(transaction.getCardId(), amount);

        log.info("Transaction settled successfully: {}", transaction.getTransactionId());
    }

    // ==================== Private validation methods ====================

    private void validateCard(CardId cardId, CardData cardData) {
        if (!cardValidationPort.isCardActive(cardId)) {
            throw CardValidationException.invalidStatus(cardId, "INACTIVE");
        }

        if (cardValidationPort.isCardBlocked(cardId)) {
            throw CardValidationException.blocked(cardId);
        }

        if (cardData.isExpired()) {
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

    private void checkAvailableCredit(CardId cardId, Money requestedAmount) {
        if (!creditLimitPort.hasSufficientFunds(cardId, requestedAmount)) {
            Money available = creditLimitPort.getAvailableCredit(cardId);
            throw new InsufficientFundsException(cardId, requestedAmount, available);
        }
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
