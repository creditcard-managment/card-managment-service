package com.unisono.card.payment.domain.model;

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
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.cardData = cardData;
        this.merchantInfo = merchantInfo;
        this.type = type;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = TransactionStatus.PENDING;
        this.transactionDate = LocalDateTime.now();
        this.version = 0;
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
     * Decline the transaction
     */
    public void decline(String reason) {
        validateNotFinal();

        this.status = TransactionStatus.DECLINED;
        this.declineReason = reason;

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
     * Mark transaction as completed
     */
    public void complete() {
        validateNotFinal();

        addDomainEvent(new TransactionCompleted(
                this.transactionId,
                this.cardId,
                this.status,
                LocalDateTime.now()
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
