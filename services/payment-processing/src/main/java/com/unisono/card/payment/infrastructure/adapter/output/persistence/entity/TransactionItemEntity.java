package com.unisono.card.payment.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for Transaction Items (line items in a transaction)
 */
@Entity
@Table(
        name = "transaction_items",
        indexes = {
                @Index(name = "idx_transaction_items_transaction_id", columnList = "transaction_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionItemEntity {

    @Id
    @Column(name = "item_id", nullable = false, updatable = false)
    @Builder.Default
    private UUID itemId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private TransactionEntity transaction;

    @Column(name = "item_description", length = 500)
    private String itemDescription;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (itemId == null) {
            itemId = UUID.randomUUID();
        }
        // Calculate total if not set
        if (totalAmount == null && unitPrice != null && quantity != null) {
            totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionItemEntity)) return false;
        return itemId != null && itemId.equals(((TransactionItemEntity) o).getItemId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
