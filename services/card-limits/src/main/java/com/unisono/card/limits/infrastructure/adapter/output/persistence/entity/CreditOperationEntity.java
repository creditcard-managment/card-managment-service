package com.unisono.card.limits.infrastructure.adapter.output.persistence.entity;

import com.unisono.card.limits.domain.model.CreditOperation;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity para credit_operations (operaciones de crédito idempotentes, V12)
 */
@Entity
@Table(name = "credit_operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditOperationEntity {

    @Id
    @Column(name = "operation_id", length = 64)
    private String operationId;

    @Column(name = "credit_limit_id", nullable = false, length = 36)
    private String creditLimitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", length = 30)
    private CreditOperation.Type operationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CreditOperation.Status status;

    @Column(name = "credit_delta", nullable = false, precision = 15, scale = 2)
    private BigDecimal creditDelta;

    @Column(name = "daily_delta", nullable = false, precision = 15, scale = 2)
    private BigDecimal dailyDelta;

    @Column(name = "monthly_delta", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyDelta;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "applied_on", nullable = false)
    private LocalDate appliedOn;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
}
