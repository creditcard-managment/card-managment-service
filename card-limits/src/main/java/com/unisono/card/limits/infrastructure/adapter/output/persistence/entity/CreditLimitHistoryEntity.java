package com.unisono.card.limits.infrastructure.adapter.output.persistence.entity;

import com.unisono.card.limits.domain.model.LimitType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity para CreditLimitHistory
 */
@Entity
@Table(name = "credit_limit_history", indexes = {
        @Index(name = "idx_history_credit_limit_id", columnList = "credit_limit_id"),
        @Index(name = "idx_history_changed_at", columnList = "changed_at"),
        @Index(name = "idx_history_action", columnList = "action")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditLimitHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "credit_limit_id", nullable = false, length = 36)
    private String creditLimitId;

    // Tipo de cambio
    @Enumerated(EnumType.STRING)
    @Column(name = "limit_type", nullable = false, length = 20)
    private LimitType limitType;

    @Column(name = "action", nullable = false, length = 20)
    private String action; // ASSIGNED, INCREASED, DECREASED, UPDATED

    // Valores
    @Column(name = "previous_value_amount", precision = 15, scale = 2)
    private BigDecimal previousValueAmount;

    @Column(name = "previous_value_currency", length = 3)
    private String previousValueCurrency;

    @Column(name = "new_value_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal newValueAmount;

    @Column(name = "new_value_currency", nullable = false, length = 3)
    private String newValueCurrency;

    @Column(name = "difference_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal differenceAmount;

    @Column(name = "difference_currency", nullable = false, length = 3)
    private String differenceCurrency;

    // Contexto
    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    // Timestamp
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    // Metadata adicional (JSON)
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
}
