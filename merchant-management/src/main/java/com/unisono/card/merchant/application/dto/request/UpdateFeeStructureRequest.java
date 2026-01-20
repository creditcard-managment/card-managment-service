package com.unisono.card.merchant.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO Request para actualizar estructura de fees
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFeeStructureRequest {

    @NotEmpty(message = "At least one transaction fee is required")
    private List<TransactionFeeDto> transactionFees;

    @NotNull(message = "Monthly fee is required")
    @DecimalMin(value = "0.0", message = "Monthly fee cannot be negative")
    private BigDecimal monthlyFee;

    @DecimalMin(value = "0.0", message = "Setup fee cannot be negative")
    @Builder.Default
    private BigDecimal setupFee = BigDecimal.ZERO;

    @NotNull(message = "Chargeback fee is required")
    @DecimalMin(value = "0.01", message = "Chargeback fee must be greater than 0")
    private BigDecimal chargebackFee;

    private LocalDateTime effectiveFrom;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionFeeDto {

        @NotBlank(message = "Fee type is required")
        private String feeType;

        @DecimalMin(value = "0.0", message = "Percentage fee cannot be negative")
        @DecimalMax(value = "100.0", message = "Percentage fee cannot exceed 100%")
        private BigDecimal percentageFee;

        @DecimalMin(value = "0.0", message = "Fixed fee cannot be negative")
        private BigDecimal fixedFee;

        @NotEmpty(message = "At least one card brand must be specified")
        private List<String> applicableCardBrands;

        @NotEmpty(message = "At least one transaction type must be specified")
        private List<String> applicableTransactionTypes;

        @DecimalMin(value = "0.0", message = "Minimum fee cannot be negative")
        @Builder.Default
        private BigDecimal minimumFee = BigDecimal.ZERO;

        private BigDecimal maximumFee;
    }
}
