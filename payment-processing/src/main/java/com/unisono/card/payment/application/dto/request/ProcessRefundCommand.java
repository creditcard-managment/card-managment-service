package com.unisono.card.payment.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Command for processing a refund transaction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessRefundCommand {

    @NotBlank(message = "Original transaction ID is required")
    private String originalTransactionId;

    @NotBlank(message = "Card ID is required")
    private String cardId;

    @NotNull(message = "Refund amount is required")
    @DecimalMin(value = "0.01", message = "Refund amount must be greater than zero")
    private BigDecimal refundAmount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    private String currency;

    @NotBlank(message = "Merchant ID is required")
    private String merchantId;

    @NotBlank(message = "Refund reason is required")
    @Size(max = 500, message = "Refund reason cannot exceed 500 characters")
    private String reason;

    private boolean isPartialRefund;

    private String initiatedBy; // User/system who initiated the refund
}
