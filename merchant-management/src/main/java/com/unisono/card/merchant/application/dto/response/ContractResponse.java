package com.unisono.card.merchant.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.unisono.card.merchant.domain.model.contract.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

/**
 * DTO Response para contrato
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractResponse {

    private String id;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private ContractStatus status;
    private boolean autoRenew;
    private Integer renewalPeriodMonths;

    // Contract Terms
    private ContractTermsDto terms;

    // Chargeback Policy
    private ChargebackPolicyDto chargebackPolicy;

    // Service Level
    private ServiceLevelDto serviceLevel;

    // Calculated fields
    private Long daysRemaining;
    private boolean isExpiringSoon;

    public static ContractResponse from(MerchantContract contract) {
        return ContractResponse.builder()
                .id(contract.getId().getValue())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .status(contract.getStatus())
                .autoRenew(contract.isAutoRenew())
                .renewalPeriodMonths(contract.getRenewalPeriodMonths())
                .terms(ContractTermsDto.from(contract.getTerms()))
                .chargebackPolicy(ChargebackPolicyDto.from(contract.getChargebackPolicy()))
                .serviceLevel(ServiceLevelDto.from(contract.getServiceLevel()))
                .daysRemaining(contract.getDaysRemaining())
                .isExpiringSoon(contract.isExpiringSoon(30))
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractTermsDto {
        private Integer settlementDays;
        private BigDecimal minimumMonthlyVolume;
        private BigDecimal maximumMonthlyVolume;
        private BigDecimal reservePercentage;
        private Integer earlyTerminationNoticeDays;
        private String legalJurisdiction;
        private String disputeResolutionMethod;

        public static ContractTermsDto from(ContractTerms terms) {
            return ContractTermsDto.builder()
                    .settlementDays(terms.getSettlementDays())
                    .minimumMonthlyVolume(terms.getMinimumMonthlyVolume())
                    .maximumMonthlyVolume(terms.getMaximumMonthlyVolume())
                    .reservePercentage(terms.getReservePercentage())
                    .earlyTerminationNoticeDays(terms.getEarlyTerminationNoticeDays())
                    .legalJurisdiction(terms.getLegalJurisdiction())
                    .disputeResolutionMethod(terms.getDisputeResolutionMethod())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChargebackPolicyDto {
        private BigDecimal chargebackFee;
        private BigDecimal maxChargebackRatio;
        private Integer warningThresholdCount;
        private BigDecimal warningThresholdRatio;
        private Integer reviewPeriodDays;
        private boolean automaticSuspensionEnabled;

        public static ChargebackPolicyDto from(ChargebackPolicy policy) {
            return ChargebackPolicyDto.builder()
                    .chargebackFee(policy.getChargebackFee())
                    .maxChargebackRatio(policy.getMaxChargebackRatio())
                    .warningThresholdCount(policy.getWarningThresholdCount())
                    .warningThresholdRatio(policy.getWarningThresholdRatio())
                    .reviewPeriodDays(policy.getReviewPeriodDays())
                    .automaticSuspensionEnabled(policy.isAutomaticSuspensionEnabled())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceLevelDto {
        private String tier;
        private BigDecimal uptimeGuarantee;
        private Integer supportResponseTimeMinutes;
        private Integer disputeResolutionDays;
        private boolean dedicatedAccountManager;
        private boolean prioritySupport;
        private Integer maxTransactionsPerDay;
        private BigDecimal maxTransactionAmount;

        public static ServiceLevelDto from(ServiceLevel serviceLevel) {
            return ServiceLevelDto.builder()
                    .tier(serviceLevel.getTier().name())
                    .uptimeGuarantee(serviceLevel.getUptimeGuarantee())
                    .supportResponseTimeMinutes(serviceLevel.getSupportResponseTimeMinutes())
                    .disputeResolutionDays(serviceLevel.getDisputeResolutionDays())
                    .dedicatedAccountManager(serviceLevel.isDedicatedAccountManager())
                    .prioritySupport(serviceLevel.isPrioritySupport())
                    .maxTransactionsPerDay(serviceLevel.getMaxTransactionsPerDay())
                    .maxTransactionAmount(serviceLevel.getMaxTransactionAmount())
                    .build();
        }
    }
}
