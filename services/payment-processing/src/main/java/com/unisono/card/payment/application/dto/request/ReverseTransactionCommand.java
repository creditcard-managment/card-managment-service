package com.unisono.card.payment.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Command for reversing a transaction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReverseTransactionCommand {

    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    @NotBlank(message = "Reversal reason is required")
    @Size(max = 500, message = "Reversal reason cannot exceed 500 characters")
    private String reason;

    private String reversalType; // TIMEOUT, ERROR, CUSTOMER_REQUEST, FRAUD, etc.

    private String initiatedBy;
}
