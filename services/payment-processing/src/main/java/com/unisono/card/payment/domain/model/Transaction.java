package com.unisono.card.payment.domain.model;

import lombok.Builder;
import java.util.Objects;
import com.unisono.card.events.domain.DomainEvent; // ✅ Importar DomainEvent
import com.unisono.card.payment.domain.event.*;
import com.unisono.card.payment.domain.exception.InvalidTransactionException;
import com.unisono.card.payment.domain.exception.TransactionAlreadyProcessedException;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.AccessLevel;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root: Transaction
 * Represents a payment transaction in the system
 */
@Getter
public class Transaction {

    private final TransactionId transactionId;
    private final CardId cardId;
    private final CardData cardData;
    private final MerchantInfo merchantInfo;
    private final TransactionType type;
    private final TransactionAmount amount;
    private final PaymentMethod paymentMethod;

    /**
     * Refunded transaction (only for REFUND)
     */
    private TransactionId originalTransactionId;

    private TransactionStatus status;
    private AuthorizationCode authorizationCode;
    private Integer riskScore;
    private String declineReason;
    private String terminalId;
    private String posEntryMode;

    private final LocalDateTime transactionDate;
    private LocalDateTime settlementDate;

    @Getter(AccessLevel.NONE)
    private final List<DomainEvent> domainEvents = new ArrayList<>(); // ✅ Cambiar de Object a DomainEvent

    private int version;

    // Constructor privado - usar factory methods
    private Transaction(TransactionId transactionId, CardId cardId, CardData cardData,
                        MerchantInfo merchantInfo, TransactionType type, TransactionAmount amount,
                        PaymentMethod paymentMethod) {
        this(transactionId, cardId, cardData, merchantInfo, type, amount, paymentMethod, LocalDateTime.now());
    }

    private Transaction(TransactionId transactionId, CardId cardId, CardData cardData,
                        MerchantInfo merchantInfo, TransactionType type, TransactionAmount amount,
                        PaymentMethod paymentMethod, LocalDateTime transactionDate) {
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.cardData = cardData;
        this.merchantInfo = merchantInfo;
        this.type = type;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = TransactionStatus.PENDING;
        this.transactionDate = transactionDate;
        this.version = 0;
    }

    /**
     * Rebuilds a stored transaction with its saved state. Unlike the {@code create*} factories it does not
     * apply business rules nor emit domain events: the transaction already happened.
     */
    public static Transaction reconstitute(PersistedState state) {
        Objects.requireNonNull(state, "state");
        Transaction transaction = new Transaction(
                Objects.requireNonNull(state.transactionId(), "transactionId"),
                Objects.requireNonNull(state.cardId(), "cardId"),
                Objects.requireNonNull(state.cardData(), "cardData"),
                Objects.requireNonNull(state.merchantInfo(), "merchantInfo"),
                Objects.requireNonNull(state.type(), "type"),
                Objects.requireNonNull(state.amount(), "amount"),
                Objects.requireNonNull(state.paymentMethod(), "paymentMethod"),
                Objects.requireNonNull(state.transactionDate(), "transactionDate")
        );
        transaction.status = Objects.requireNonNull(state.status(), "status");
        transaction.authorizationCode = state.authorizationCode();
        transaction.riskScore = state.riskScore();
        transaction.declineReason = state.declineReason();
        transaction.terminalId = state.terminalId();
        transaction.posEntryMode = state.posEntryMode();
        transaction.settlementDate = state.settlementDate();
        transaction.originalTransactionId = state.originalTransactionId();
        transaction.version = state.version();
        return transaction;
    }

    /**
     * Stored state of a transaction, used by {@link #reconstitute(PersistedState)}.
     * {@code originalTransactionId} is only set for refunds (and may be missing in refunds stored before V11).
     */
    @Builder
    public record PersistedState(
            TransactionId transactionId,
            CardId cardId,
            CardData cardData,
            MerchantInfo merchantInfo,
            TransactionType type,
            TransactionAmount amount,
            PaymentMethod paymentMethod,
            TransactionStatus status,
            AuthorizationCode authorizationCode,
            Integer riskScore,
            String declineReason,
            String terminalId,
            String posEntryMode,
            LocalDateTime transactionDate,
            LocalDateTime settlementDate,
            TransactionId originalTransactionId,
            int version
    ) {
    }

    /**
     * Factory method: Create a new purchase transaction
     */
    public static Transaction createPurchase(CardId cardId, CardData cardData, MerchantInfo merchantInfo,
                                             TransactionAmount amount, PaymentMethod paymentMethod) {
        Transaction transaction = new Transaction(
                TransactionId.generate(),
                cardId,
                cardData,
                merchantInfo,
                TransactionType.PURCHASE,
                amount,
                paymentMethod
        );

        transaction.addDomainEvent(new PaymentInitiated(
                transaction.transactionId,
                transaction.cardId,
                transaction.amount,
                transaction.type,
                transaction.transactionDate
        ));

        return transaction;
    }

    /**
     * Factory method: Create a refund transaction
     */
    public static Transaction createCashAdvance(CardId cardId, CardData cardData, MerchantInfo merchantInfo,
                                                TransactionAmount amount, PaymentMethod paymentMethod) {
        Transaction transaction = new Transaction(
                TransactionId.generate(),
                cardId,
                cardData,
                merchantInfo,
                TransactionType.CASH_ADVANCE,
                amount,
                paymentMethod
        );

        transaction.addDomainEvent(new PaymentInitiated(
                transaction.transactionId,
                transaction.cardId,
                transaction.amount,
                transaction.type,
                transaction.transactionDate
        ));

        return transaction;
    }

    public static Transaction createRefund(CardId cardId, CardData cardData, MerchantInfo merchantInfo,
                                           TransactionAmount amount, TransactionId originalTransactionId) {
        Transaction transaction = new Transaction(
                TransactionId.generate(),
                cardId,
                cardData,
                merchantInfo,
                TransactionType.REFUND,
                amount,
                PaymentMethod.ONLINE
        );
        transaction.originalTransactionId = originalTransactionId;

        transaction.addDomainEvent(new RefundInitiated(
                transaction.transactionId,
                originalTransactionId,
                transaction.cardId,
                transaction.amount,
                transaction.transactionDate
        ));

        return transaction;
    }

    /**
     * Records the risk score evaluated by the fraud check, so a declined transaction keeps it too
     */
    public void assessRisk(Integer riskScore) {
        validateNotFinal();
        this.riskScore = riskScore;
    }

    /**
     * Authorize the transaction
     */
    public void authorize(AuthorizationCode authCode, Integer riskScore) {
        validateNotFinal();

        if (!authCode.isApproved()) {
            throw new InvalidTransactionException("Cannot authorize with a declined authorization code");
        }

        this.authorizationCode = authCode;
        this.riskScore = riskScore;
        this.status = TransactionStatus.AUTHORIZED;

        addDomainEvent(new PaymentAuthorized(
                this.transactionId,
                this.cardId,
                this.authorizationCode.getCode(),
                this.riskScore,
                authCode.getAuthorizedAt()
        ));
    }

    /**
     * Approve the transaction
     */
    public void approve() {
        if (this.status != TransactionStatus.AUTHORIZED) {
            throw new InvalidTransactionException("Transaction must be authorized before approval");
        }

        this.status = TransactionStatus.APPROVED;

        addDomainEvent(new PaymentProcessed(
                this.transactionId,
                this.cardId,
                this.amount,
                this.status,
                LocalDateTime.now()
        ));
    }

    /**
     * Approves a refund. Refunds are not authorized by the network: they go from PENDING to APPROVED.
     */
    public void approveRefund(TransactionId originalTransactionId) {
        if (this.type != TransactionType.REFUND) {
            throw new InvalidTransactionException("Only refunds can be approved without authorization");
        }
        if (this.status != TransactionStatus.PENDING) {
            throw new InvalidTransactionException(
                    String.format("Refund with status %s cannot be approved", this.status)
            );
        }

        this.status = TransactionStatus.APPROVED;

        addDomainEvent(new RefundProcessed(
                this.transactionId,
                originalTransactionId,
                this.cardId,
                this.amount,
                LocalDateTime.now()
        ));
    }

    /**
     * Decline the transaction. The declined code keeps the ISO response code (51, 55, 59, ...)
     * so the rejection is recorded and reported with it.
     */
    public void decline(String reason, AuthorizationCode declinedCode) {
        validateNotFinal();

        if (declinedCode == null || declinedCode.isApproved()) {
            throw new InvalidTransactionException("A transaction must be declined with a declined response code");
        }

        this.status = TransactionStatus.DECLINED;
        this.declineReason = reason;
        this.authorizationCode = declinedCode;

        addDomainEvent(new PaymentDeclined(
                this.transactionId,
                this.cardId,
                reason,
                LocalDateTime.now()
        ));
    }

    /**
     * Reverse the transaction
     */
    public void reverse(String reason) {
        if (!this.status.canBeReversed()) {
            throw new InvalidTransactionException(
                    String.format("Transaction with status %s cannot be reversed", this.status)
            );
        }

        this.status = TransactionStatus.REVERSED;
        this.declineReason = reason;

        addDomainEvent(new PaymentReversed(
                this.transactionId,
                this.cardId,
                this.amount,
                reason,
                LocalDateTime.now()
        ));
    }

    /**
     * Settle the transaction
     */
    public void settle() {
        if (!this.status.canBeSettled()) {
            throw new InvalidTransactionException(
                    String.format("Transaction with status %s cannot be settled", this.status)
            );
        }

        this.status = TransactionStatus.SETTLED;
        this.settlementDate = LocalDateTime.now();

        addDomainEvent(new TransactionSettled(
                this.transactionId,
                this.cardId,
                this.amount,
                this.settlementDate
        ));
    }

    /**
     * Set terminal information
     */
    public void setTerminalInfo(String terminalId, String posEntryMode) {
        this.terminalId = terminalId;
        this.posEntryMode = posEntryMode;
    }

    // Domain event management
    private void addDomainEvent(DomainEvent event) { // ✅ Cambiar parámetro de Object a DomainEvent
        this.domainEvents.add(event);
    }

    public List<DomainEvent> getDomainEvents() { // ✅ Cambiar retorno de List<Object> a List<DomainEvent>
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    // Validations
    private void validateNotFinal() {
        if (this.status.isFinal()) {
            throw new TransactionAlreadyProcessedException(
                    String.format("Transaction %s is already in final status: %s",
                            this.transactionId, this.status)
            );
        }
    }

    public boolean isSuccessful() {
        return this.status == TransactionStatus.APPROVED || this.status == TransactionStatus.SETTLED;
    }

    public boolean requiresSettlement() {
        return this.status == TransactionStatus.APPROVED && this.settlementDate == null;
    }
}
