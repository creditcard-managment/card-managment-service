package com.unisono.card.payment.domain.service;

import com.unisono.card.payment.domain.exception.*;
import com.unisono.card.payment.domain.model.*;
import com.unisono.card.payment.domain.port.output.*;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

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
     * Validate and process a debit transaction (PURCHASE or CASH_ADVANCE).
     * <p>
     * A card that does not exist or an invalid merchant are request errors and throw (the transaction
     * cannot be stored: card_id and merchant_id are foreign keys). Every other rejection (card status,
     * PIN, credit, fraud, authorization) declines the transaction, which is returned {@code DECLINED}
     * with the ISO response code so that it gets recorded and {@code PaymentDeclined} is published.
     *
     * @param verification PIN (required for CASH_ADVANCE) and CVV (required for ONLINE and MANUAL),
     *                     verified against card-data
     */
    public Transaction processPayment(CardId cardId, CardData cardData, MerchantInfo merchantInfo,
                                      TransactionAmount amount, PaymentMethod paymentMethod,
                                      TransactionType type, CardVerification verification) {
        return authorizePayment(cardId, cardData, merchantInfo, amount, paymentMethod, type, verification, true);
    }

    /**
     * Pre-authorize a purchase: same validations and credit consumption as a purchase (the consumed
     * credit is the hold), but the transaction stays {@code AUTHORIZED} until it is captured
     * ({@link #captureTransaction}) or reversed ({@link #reverseTransaction}, which releases the hold).
     */
    public Transaction preAuthorize(CardId cardId, CardData cardData, MerchantInfo merchantInfo,
                                    TransactionAmount amount, PaymentMethod paymentMethod,
                                    CardVerification verification) {
        return authorizePayment(cardId, cardData, merchantInfo, amount, paymentMethod,
                TransactionType.PURCHASE, verification, false);
    }

    /**
     * Capture a pre-authorized transaction: AUTHORIZED → APPROVED. The credit was already consumed
     * when it was pre-authorized.
     */
    public void captureTransaction(Transaction transaction) {
        log.info("Capturing transaction: {}", transaction.getTransactionId());

        if (transaction.getStatus() != TransactionStatus.AUTHORIZED) {
            throw new InvalidTransactionException(
                    transaction.getTransactionId(),
                    "Only authorized transactions can be captured"
            );
        }

        try {
            authorizationPort.captureAuthorization(transaction);
        } catch (RuntimeException ex) {
            throw new ExternalServiceException("authorization", ex.getMessage(), ex);
        }
        transaction.approve();

        log.info("Transaction captured successfully: {}", transaction.getTransactionId());
    }

    private Transaction authorizePayment(CardId cardId, CardData cardData, MerchantInfo merchantInfo,
                                         TransactionAmount amount, PaymentMethod paymentMethod,
                                         TransactionType type, CardVerification verification,
                                         boolean capture) {
        CardVerification cardVerification = verification != null ? verification : CardVerification.none();

        log.info("Processing {} for card: {}, merchant: {}, amount: {}{}",
                type, cardId, merchantInfo.getMerchantId(), amount, capture ? "" : " (pre-authorization)");

        // 1. Card must exist and merchant must be valid (not recordable otherwise)
        CardValidationPort.CardStatusSnapshot card = findCard(cardId);
        validateMerchant(merchantInfo);

        // 2. Create transaction
        Transaction transaction = type == TransactionType.CASH_ADVANCE
                ? Transaction.createCashAdvance(cardId, cardData, merchantInfo, amount, paymentMethod)
                : Transaction.createPurchase(cardId, cardData, merchantInfo, amount, paymentMethod);

        try {
            // 3. Card status and expiration
            validateCardStatus(cardId, card, cardData);

            // 4. Cardholder verification: PIN (cash advance) and CVV (card not present, or whenever it is sent)
            if (type == TransactionType.CASH_ADVANCE) {
                verifyPin(cardId, cardVerification);
            }
            if (paymentMethod.requiresCVV() || cardVerification.hasCvv()) {
                verifyCvv(cardId, cardVerification);
            }

            // 5. Check available credit
            checkAvailableCredit(cardId, amount.getEffectiveAmount(), type);

            // 6. Fraud check (the score is calculated once and kept in the transaction)
            int riskScore = performFraudCheck(transaction);

            // 7. Request authorization. A declined code stops here, before touching the credit
            AuthorizationCode authCode = requestAuthorization(transaction);
            if (!authCode.isApproved()) {
                log.warn("Authorization declined for transaction {}: response code {}",
                        transaction.getTransactionId(), authCode.getResponseCode());
                transaction.decline("Authorization declined by the card network (response code "
                        + authCode.getResponseCode() + ")", authCode);
                return transaction;
            }

            // 8. Authorize transaction
            transaction.authorize(authCode, riskScore);

            // 9. Consume credit in card-limits
            creditLimitPort.consumeCredit(transaction.getTransactionId(), cardId, amount.getEffectiveAmount(), type,
                    creditDescription(transaction));

            // 10. Approve transaction (a pre-authorization stays AUTHORIZED until captured)
            if (capture) {
                transaction.approve();
                log.info("Payment processed successfully: {}", transaction.getTransactionId());
            } else {
                log.info("Payment pre-authorized: {}", transaction.getTransactionId());
            }

        } catch (CardValidationException | InsufficientFundsException
                 | FraudDetectedException | AuthorizationFailedException ex) {
            decline(transaction, ex);
        }

        return transaction;
    }

    /**
     * Process a refund of an approved or settled purchase / cash advance.
     * <p>
     * The refunded total is controlled against the original: previous refunds are subtracted, so a partial
     * refund cannot exceed what is left and a full refund refunds exactly what is left. The refund keeps
     * the link to the original transaction.
     *
     * @param original        refunded transaction (loaded with a lock, so concurrent refunds are serialized)
     * @param previousRefunds refunds already registered for the original transaction
     * @param fullRefund      true to refund everything not yet refunded
     */
    public Transaction processRefund(Transaction original, CardId cardId, TransactionAmount amount,
                                     List<Transaction> previousRefunds, boolean fullRefund) {

        TransactionId originalTransactionId = original.getTransactionId();
        log.info("Processing {} refund for original transaction: {}, amount: {}",
                fullRefund ? "full" : "partial", originalTransactionId, amount);

        // 1. Original transaction must be refundable and belong to the card
        validateCanRefund(original, cardId);

        // 2. Amount against what is left to refund
        Money remaining = remainingToRefund(original, previousRefunds);
        Money requested = amount.getEffectiveAmount();

        if (!remaining.isPositive()) {
            throw new InvalidTransactionException(originalTransactionId, "Transaction is already fully refunded");
        }
        if (fullRefund && requested.getAmount().compareTo(remaining.getAmount()) != 0) {
            throw new InvalidTransactionException(originalTransactionId, String.format(
                    "Full refund amount %s must match the amount not yet refunded: %s", requested, remaining));
        }
        if (requested.isGreaterThan(remaining)) {
            throw new InvalidTransactionException(originalTransactionId, String.format(
                    "Refund amount %s exceeds the amount not yet refunded: %s", requested, remaining));
        }

        // 3. Validate card
        validateCard(cardId, original.getCardData());

        // 4. Create refund transaction linked to the original
        Transaction refundTransaction = Transaction.createRefund(
                cardId, original.getCardData(), original.getMerchantInfo(), amount, originalTransactionId
        );

        // 5. Auto-approve refunds (no authorization needed)
        refundTransaction.approveRefund(originalTransactionId);

        // 6. Undo the consumption of the original (available credit, daily and monthly quota)
        creditLimitPort.restoreCredit(refundTransaction.getTransactionId(), cardId, requested,
                original.getTransactionDate().toLocalDate(),
                "Refund " + refundTransaction.getTransactionId() + " of " + originalTransactionId);

        log.info("Refund processed successfully: {}", refundTransaction.getTransactionId());
        return refundTransaction;
    }

    /**
     * Amount of the original transaction that has not been refunded yet
     */
    public Money remainingToRefund(Transaction original, List<Transaction> previousRefunds) {
        Money originalAmount = original.getAmount().getEffectiveAmount();
        Money refunded = previousRefunds.stream()
                .filter(Transaction::isSuccessful)
                .map(refund -> refund.getAmount().getEffectiveAmount())
                .reduce(Money.zero(originalAmount.getCurrencyCode()), Money::add);
        return originalAmount.subtract(refunded);
    }

    /**
     * Reverse a transaction
     */
    public void reverseTransaction(Transaction transaction, String reason) {
        log.info("Reversing transaction: {}, reason: {}", transaction.getTransactionId(), reason);

        // 1. Reverse the transaction. A pre-authorization that was not captured is voided in the network
        boolean preAuthorized = transaction.getStatus() == TransactionStatus.AUTHORIZED;
        transaction.reverse(reason);
        if (preAuthorized) {
            voidAuthorization(transaction);
        }

        // 2. Undo the consumption (available credit, daily and monthly quota)
        Money amount = transaction.getAmount().getEffectiveAmount();
        creditLimitPort.restoreCredit(transaction.getTransactionId(), transaction.getCardId(), amount,
                transaction.getTransactionDate().toLocalDate(),
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
        validateCardStatus(cardId, findCard(cardId), cardData);
    }

    private CardValidationPort.CardStatusSnapshot findCard(CardId cardId) {
        return cardValidationPort.findCardStatus(cardId)
                .orElseThrow(() -> CardValidationException.notFound(cardId));
    }

    private void validateCardStatus(CardId cardId, CardValidationPort.CardStatusSnapshot card, CardData cardData) {
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

    private void validateCanRefund(Transaction original, CardId cardId) {
        if (!original.getType().isDebit()) {
            throw new InvalidTransactionException(original.getTransactionId(),
                    String.format("Transactions of type %s cannot be refunded", original.getType()));
        }
        if (!original.isSuccessful()) {
            throw new InvalidTransactionException(original.getTransactionId(),
                    "Only approved or settled transactions can be refunded");
        }
        if (!original.getCardId().equals(cardId)) {
            throw new InvalidTransactionException(original.getTransactionId(),
                    "Refund card does not match the card of the original transaction");
        }
    }

    private void verifyPin(CardId cardId, CardVerification verification) {
        if (!verification.hasPin()) {
            throw CardValidationException.pinRequired(cardId);
        }
        if (!cardValidationPort.validatePin(cardId, verification.pin())) {
            throw CardValidationException.invalidPin(cardId);
        }
    }

    private void verifyCvv(CardId cardId, CardVerification verification) {
        if (!verification.hasCvv()) {
            throw CardValidationException.cvvRequired(cardId);
        }
        if (!cardValidationPort.validateCvv(cardId, verification.cvv())) {
            throw CardValidationException.invalidCVV(cardId);
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

    /**
     * Calculates the risk score once, records it in the transaction and evaluates it
     *
     * @return the evaluated risk score
     */
    private int performFraudCheck(Transaction transaction) {
        Integer score = fraudCheckPort.calculateRiskScore(transaction);
        int riskScore = score != null ? score : 0;
        transaction.assessRisk(riskScore);

        if (fraudCheckPort.isSuspicious(transaction, riskScore)) {
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

        return riskScore;
    }

    /**
     * Declines the transaction with the reason and ISO response code of the rejection.
     * If it had already been authorized (credit consumption failed afterwards), the authorization is voided.
     */
    private void decline(Transaction transaction, PaymentException rejection) {
        String responseCode = responseCodeFor(rejection);
        log.warn("Declining transaction {} (response code {}): {}",
                transaction.getTransactionId(), responseCode, rejection.getMessage());

        if (transaction.getStatus() == TransactionStatus.AUTHORIZED) {
            voidAuthorization(transaction);
        }

        transaction.decline(rejection.getMessage(), AuthorizationCode.declined(responseCode));
    }

    private void voidAuthorization(Transaction transaction) {
        try {
            authorizationPort.voidAuthorization(transaction);
        } catch (Exception ex) {
            log.error("Could not void authorization of transaction {}", transaction.getTransactionId(), ex);
        }
    }

    /**
     * ISO 8583 response code for each kind of rejection
     */
    private static String responseCodeFor(PaymentException rejection) {
        if (rejection instanceof AuthorizationFailedException authorization) {
            return authorization.getResponseCode();
        }
        if (rejection instanceof InsufficientFundsException) {
            return "51"; // Insufficient funds / limit exceeded
        }
        if (rejection instanceof FraudDetectedException) {
            return "59"; // Suspected fraud
        }
        if (rejection instanceof CardValidationException card) {
            if ("EXPIRATION".equals(card.getValidationType())) {
                return "54"; // Expired card
            }
            if ("PIN".equals(card.getValidationType())) {
                return "55"; // Incorrect PIN
            }
            if ("CVV".equals(card.getValidationType())) {
                return "N7"; // CVV2 failure
            }
        }
        return "62"; // Restricted card
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
