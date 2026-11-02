package com.unisono.card.limits.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * JPA Entity para CreditLimit
 */
@Entity
@Table(name = "credit_limits", indexes = {
        @Index(name = "idx_credit_limit_card_id", columnList = "card_id"),
        @Index(name = "idx_credit_limit_deleted", columnList = "deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditLimitEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "card_id", nullable = false, length = 36, unique = true)
    private String cardId;

    // ========================================
    // Límites principales
    // ========================================

    @Column(name = "purchase_limit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal purchaseLimitAmount;

    @Column(name = "purchase_limit_currency", nullable = false, length = 3)
    private String purchaseLimitCurrency;

    @Column(name = "cash_advance_limit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal cashAdvanceLimitAmount;

    @Column(name = "cash_advance_limit_currency", nullable = false, length = 3)
    private String cashAdvanceLimitCurrency;

    // ========================================
    // Crédito actual
    // ========================================

    @Column(name = "consumed_credit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal consumedCreditAmount;

    @Column(name = "consumed_credit_currency", nullable = false, length = 3)
    private String consumedCreditCurrency;

    @Column(name = "available_credit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal availableCreditAmount;

    @Column(name = "available_credit_currency", nullable = false, length = 3)
    private String availableCreditCurrency;

    // ========================================
    // Límites temporales - Diario
    // ========================================

    @Column(name = "daily_limit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal dailyLimitAmount;

    @Column(name = "daily_limit_currency", nullable = false, length = 3)
    private String dailyLimitCurrency;

    @Column(name = "daily_limit_consumed", nullable = false, precision = 15, scale = 2)
    private BigDecimal dailyLimitConsumed;

    @Column(name = "daily_limit_reset_date", nullable = false)
    private LocalDate dailyLimitResetDate;

    // ========================================
    // Límites temporales - Mensual
    // ========================================

    @Column(name = "monthly_limit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyLimitAmount;

    @Column(name = "monthly_limit_currency", nullable = false, length = 3)
    private String monthlyLimitCurrency;

    @Column(name = "monthly_limit_consumed", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyLimitConsumed;

    @Column(name = "monthly_limit_reset_year", nullable = false)
    private Integer monthlyLimitResetYear;

    @Column(name = "monthly_limit_reset_month", nullable = false)
    private Integer monthlyLimitResetMonth;

    // ========================================
    // Auditoría
    // ========================================

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Soft delete
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ========================================
    // Helper methods
    // ========================================

    @PrePersist
    protected void onCreate() {
        if (deleted == null) {
            deleted = false;
        }
    }

    public void markAsDeleted() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    // Helper para reconstruir YearMonth
    public YearMonth getMonthlyLimitResetYearMonth() {
        return YearMonth.of(monthlyLimitResetYear, monthlyLimitResetMonth);
    }

    public void setMonthlyLimitResetYearMonth(YearMonth yearMonth) {
        this.monthlyLimitResetYear = yearMonth.getYear();
        this.monthlyLimitResetMonth = yearMonth.getMonthValue();
    }
}
