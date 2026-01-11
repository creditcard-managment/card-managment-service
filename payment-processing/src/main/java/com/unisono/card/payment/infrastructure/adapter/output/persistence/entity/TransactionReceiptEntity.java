package com.unisono.card.payment.infrastructure.adapter.output.persistence.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * JPA Entity for Transaction Receipts
 */
@Entity
@Table(
        name = "transaction_receipts",
        indexes = {
                @Index(name = "idx_receipts_transaction_id", columnList = "transaction_id"),
                @Index(name = "idx_receipts_receipt_number", columnList = "receipt_number", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionReceiptEntity {

    @Id
    @Column(name = "receipt_id", nullable = false, updatable = false)
    @Builder.Default
    private UUID receiptId = UUID.randomUUID();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private TransactionEntity transaction;

    @Column(name = "receipt_number", nullable = false, unique = true, length = 50)
    private String receiptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "receipt_type", nullable = false, length = 20)
    private ReceiptType receiptType;

    @Type(JsonType.class)
    @Column(name = "receipt_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> receiptData;

    @CreationTimestamp
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "delivery_status", length = 20)
    private String deliveryStatus;

    public enum ReceiptType {
        DIGITAL,
        PRINTED,
        EMAIL,
        SMS
    }

    @PrePersist
    protected void onCreate() {
        if (receiptId == null) {
            receiptId = UUID.randomUUID();
        }
        if (receiptNumber == null) {
            receiptNumber = generateReceiptNumber();
        }
    }

    private String generateReceiptNumber() {
        return "RCP-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionReceiptEntity)) return false;
        return receiptId != null && receiptId.equals(((TransactionReceiptEntity) o).getReceiptId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
