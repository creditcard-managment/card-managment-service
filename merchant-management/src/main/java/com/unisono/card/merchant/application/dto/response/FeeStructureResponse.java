package com.unisono.card.merchant.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.unisono.card.merchant.domain.model.fees.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO Response para estructura de fees
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeStructureResponse {

    private String id;
    private List<TransactionFeeDto> transactionFees;
    private InterchangeFeeDto interchangeFee;
    private AcquirerFeeDto acquirerFee;
    private BigDecimal monthlyFee;
    private BigDecimal setupFee;
    private BigDecimal chargebackFee;
    private boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime effectiveFrom;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime effectiveTo;

    public static FeeStructureResponse from(MerchantFeeStructure feeStructure) {
        return FeeStructureResponse.builder()
                .id(feeStructure.getId().getValue())
                .transactionFees(feeStructure.getTransactionFees().stream()
                        .map(TransactionFeeDto::from)
                        .collect(Collectors.toList()))
                .interchangeFee(InterchangeFeeDto.from(feeStructure.getInterchangeFee()))
                .acquirerFee(AcquirerFeeDto.from(feeStructure.getAcquirerFee()))
                .monthlyFee(feeStructure.getMonthlyFee())
                .setupFee(feeStructure.getSetupFee())
                .chargebackFee(feeStructure.getChargebackFee())
                .isActive(feeStructure.isActive())
                .effectiveFrom(feeStructure.getEffectiveFrom())
                .effectiveTo(feeStructure.getEffectiveTo())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionFeeDto {
        private String feeType;
        private BigDecimal percentageFee;
        private BigDecimal fixedFee;
        private java.util.Set<String> applicableCardBrands;
        private java.util.Set<String> applicableTransactionTypes;
        private BigDecimal minimumFee;
        private BigDecimal maximumFee;

        public static TransactionFeeDto from(TransactionFee fee) {
            return TransactionFeeDto.builder()
                    .feeType(fee.getFeeType().name())
                    .percentageFee(fee.getPercentageFee())
                    .fixedFee(fee.getFixedFee())
                    .applicableCardBrands(fee.getApplicableCardBrands())
                    .applicableTransactionTypes(fee.getApplicableTransactionTypes())
                    .minimumFee(fee.getMinimumFee())
                    .maximumFee(fee.getMaximumFee())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterchangeFeeDto {
        private BigDecimal rate;
        private BigDecimal fixedAmount;
        private String cardBrand;

        public static InterchangeFeeDto from(InterchangeFee fee) {
            return InterchangeFeeDto.builder()
                    .rate(fee.getRate())
                    .fixedAmount(fee.getFixedAmount())
                    .cardBrand(fee.getCardBrand())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AcquirerFeeDto {
        private BigDecimal processingRate;
        private BigDecimal authorizationFee;
        private BigDecimal settlementFee;
        private BigDecimal monthlyMinimum;

        public static AcquirerFeeDto from(AcquirerFee fee) {
            return AcquirerFeeDto.builder()
                    .processingRate(fee.getProcessingRate())
                    .authorizationFee(fee.getAuthorizationFee())
                    .settlementFee(fee.getSettlementFee())
                    .monthlyMinimum(fee.getMonthlyMinimum())
                    .build();
        }
    }
}
