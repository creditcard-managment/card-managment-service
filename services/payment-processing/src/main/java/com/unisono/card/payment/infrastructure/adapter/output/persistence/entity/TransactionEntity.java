package com.unisono.card.payment.infrastructure.adapter.output.persistence.entity;

import com.unisono.card.payment.domain.model.PaymentMethod;
import com.unisono.card.payment.domain.model.TransactionStatus;
import com.unisono.card.payment.domain.model.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for Transaction
 */
@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_transactions_card_id", columnList = "card_id"),
                @Index(name = "idx_transactions_merchant_id", columnList = "merchant_id"),
                @Index(name = "idx_transactions_status", columnList = "transaction_status"),
                @Index(name = "idx_transactions_date", columnList = "transaction_date"),
                @Index(name = "idx_transactions_settlement_date", columnList = "settlement_date"),
                @Index(name = "idx_transactions_auth_code", columnList = "authorization_code")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEntity {

    @Id
    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    @Column(name = "card_id", nullable = false, length = 36)
    private String cardId;

    @Column(name = "merchant_id", nullable = false, length = 36)
    private String merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_status", nullable = false, length = 50)
    private TransactionStatus transactionStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod;

    // ========== Amount Information ==========

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "local_amount", precision = 15, scale = 2)
    private BigDecimal localAmount;

    @Column(name = "local_currency", length = 3)
    private String localCurrency;

    @Column(name = "exchange_rate", precision = 10, scale = 6)
    private BigDecimal exchangeRate;

    // ========== Card Information (PCI Compliant - Tokenized) ==========

    @Column(name = "card_number_token", nullable = false, length = 100)
    private String cardNumberToken;

    @Column(name = "card_last_four", nullable = false, length = 4)
    private String cardLastFour;

    @Column(name = "card_holder_name", length = 100)
    private String cardholderName;

    @Column(name = "expiration_date", length = 7) // MM/YYYY
    private String expirationDate;

    // ========== Authorization ==========

    @Column(name = "authorization_code", length = 50)
    private String authorizationCode;

    @Column(name = "authorization_date")
    private LocalDateTime authorizationDate;

    @Column(name = "authorization_response_code", length = 10)
    private String authorizationResponseCode;

    // ========== Merchant Information ==========

    @Column(name = "merchant_category_code", length = 4)
    private String merchantCategoryCode;

    @Column(name = "merchant_name", length = 200)
    private String merchantName;

    @Column(name = "merchant_city", length = 100)
    private String merchantCity;

    @Column(name = "merchant_country", length = 2)
    private String merchantCountry;

    // ========== Transaction Details ==========

    @Column(name = "terminal_id", length = 50)
    private String terminalId;

    @Column(name = "pos_entry_mode", length = 20)
    private String posEntryMode;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "settlement_date")
    private LocalDateTime settlementDate;

    // ========== Fraud & Risk ==========

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "fraud_check_status", length = 20)
    private String fraudCheckStatus;

    // ========== Status Information ==========

    @Column(name = "decline_reason", length = 500)
    private String declineReason;

    @Column(name = "reversal_reason", length = 500)
    private String reversalReason;

    // ========== Refunds ==========

    /**
     * Refunded transaction (only for REFUND). FK to transactions(transaction_id), V11
     */
    @Column(name = "original_transaction_id")
    private UUID originalTransactionId;

    // ========== Audit Fields ==========

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    // ========== Helper Methods ==========

    @PrePersist
    protected void onCreate() {
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
        if (version == null) {
            version = 0;
        }
    }
}
