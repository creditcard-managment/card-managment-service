package com.unisono.card.merchant.infrastructure.adapter.persistence.jpa.entity;

import com.unisono.card.merchant.domain.model.contract.ContractStatus;
import com.unisono.card.merchant.domain.model.contract.ServiceTier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity para MerchantContract
 */
@Entity
@Table(
        name = "merchant_contracts",
        indexes = {
                @Index(name = "idx_contract_merchant", columnList = "merchant_id"),
                @Index(name = "idx_contract_status", columnList = "status"),
                @Index(name = "idx_contract_end_date", columnList = "end_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantContractEntity {

    @Id
    @Column(length = 36)
    private String id;

    // Relationship - CRITICAL: NO CASCADE aquí
    @OneToOne
    @JoinColumn(name = "merchant_id", nullable = false, unique = true)
    private MerchantEntity merchant;

    // Contract dates
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private ContractStatus status;

    @Column(name = "auto_renew", nullable = false)
    private boolean autoRenew;

    @Column(name = "renewal_period_months")
    @Builder.Default
    private Integer renewalPeriodMonths = 12;

    // Contract Terms
    @Column(name = "settlement_days", nullable = false)
    @Builder.Default
    private Integer settlementDays = 2;

    @Column(name = "minimum_monthly_volume", precision = 15, scale = 2)
    private BigDecimal minimumMonthlyVolume;

    @Column(name = "maximum_monthly_volume", precision = 15, scale = 2)
    private BigDecimal maximumMonthlyVolume;

    @Column(name = "reserve_percentage", precision = 5, scale = 2)
    private BigDecimal reservePercentage;

    @Column(name = "early_termination_notice_days")
    @Builder.Default
    private Integer earlyTerminationNoticeDays = 30;

    @Column(name = "legal_jurisdiction", length = 100)
    private String legalJurisdiction;

    @Column(name = "dispute_resolution_method", length = 100)
    private String disputeResolutionMethod;

    // Chargeback Policy
    @Column(name = "chargeback_fee", precision = 10, scale = 2)
    private BigDecimal chargebackFee;

    @Column(name = "max_chargeback_ratio", precision = 5, scale = 2)
    private BigDecimal maxChargebackRatio;

    @Column(name = "warning_threshold_count")
    private Integer warningThresholdCount;

    @Column(name = "warning_threshold_ratio", precision = 5, scale = 2)
    private BigDecimal warningThresholdRatio;

    @Column(name = "review_period_days")
    private Integer reviewPeriodDays;

    @Column(name = "automatic_suspension_enabled")
    private boolean automaticSuspensionEnabled;

    // Service Level
    @Enumerated(EnumType.STRING)
    @Column(name = "service_tier", nullable = false, length = 50)
    private ServiceTier serviceTier;

    @Column(name = "uptime_guarantee", precision = 5, scale = 2)
    private BigDecimal uptimeGuarantee;

    @Column(name = "support_response_time_minutes")
    private Integer supportResponseTimeMinutes;

    @Column(name = "dispute_resolution_days")
    private Integer disputeResolutionDays;

    @Column(name = "dedicated_account_manager")
    private boolean dedicatedAccountManager;

    @Column(name = "priority_support")
    private boolean prioritySupport;

    @Column(name = "max_transactions_per_day")
    private Integer maxTransactionsPerDay;

    @Column(name = "max_transaction_amount", precision = 15, scale = 2)
    private BigDecimal maxTransactionAmount;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Optimistic Locking
    @Version
    private Integer version;
}
