package com.unisono.card.merchant.infrastructure.adapter.persistence.jpa.entity;

import com.unisono.card.merchant.domain.model.terminal.TerminalStatus;
import com.unisono.card.merchant.domain.model.terminal.TerminalType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * JPA Entity para Payment Terminal
 * Usa estrategia SINGLE_TABLE para herencia
 */
@Entity
@Table(name = "payment_terminals", indexes = {
        @Index(name = "idx_terminal_merchant", columnList = "merchant_id"),
        @Index(name = "idx_terminal_serial", columnList = "serial_number"),
        @Index(name = "idx_terminal_status", columnList = "status"),
        @Index(name = "idx_terminal_type", columnList = "terminal_type")
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "terminal_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class PaymentTerminalEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private MerchantEntity merchant;

    @Column(name = "serial_number", nullable = false, unique = true, length = 50)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private TerminalStatus status;

    // Terminal Configuration
    @ElementCollection
    @CollectionTable(name = "terminal_accepted_brands", joinColumns = @JoinColumn(name = "terminal_id"))
    @Column(name = "card_brand")
    private Set<String> acceptedCardBrands;

    @Column(name = "max_transaction_amount", precision = 15, scale = 2)
    private BigDecimal maxTransactionAmount;

    @Column(name = "min_transaction_amount", precision = 15, scale = 2)
    private BigDecimal minTransactionAmount;

    @Column(name = "allow_refunds")
    private boolean allowRefunds;

    @Column(name = "allow_voids")
    private boolean allowVoids;

    @Column(name = "allow_partial_approvals")
    private boolean allowPartialApprovals;

    @Column(name = "require_signature")
    private boolean requireSignature;

    @Column(name = "signature_threshold", precision = 15, scale = 2)
    private BigDecimal signatureThreshold;

    @Column(name = "require_pin")
    private boolean requirePin;

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds;

    @Column(name = "currency", length = 3)
    private String currency;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Optimistic Locking
    @Version
    @Column(name = "version")
    private Integer version;

    @Transient
    public abstract TerminalType getTerminalType();
}
