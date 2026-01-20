package com.unisono.card.merchant.application.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO Request para renovar contrato
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenewContractRequest {

    @NotNull(message = "New end date is required")
    @Future(message = "End date must be in the future")
    private LocalDate newEndDate;

    @Builder.Default
    private boolean autoRenew = true;

    @Builder.Default
    private Integer renewalPeriodMonths = 12;

    private String notes;
}
