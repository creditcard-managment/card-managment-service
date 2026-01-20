package com.unisono.card.merchant.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

/**
 * DTO Request para actualizar configuración de terminal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTerminalConfigurationRequest {

    @NotEmpty(message = "At least one card brand must be accepted")
    private Set<String> acceptedCardBrands;

    @NotNull(message = "Maximum transaction amount is required")
    @DecimalMin(value = "0.01", message = "Maximum amount must be greater than 0")
    private BigDecimal maxTransactionAmount;

    @NotNull(message = "Minimum transaction amount is required")
    @DecimalMin(value = "0.01", message = "Minimum amount must be greater than 0")
    private BigDecimal minTransactionAmount;

    @Builder.Default
    private boolean allowRefunds = true;

    @Builder.Default
    private boolean allowVoids = true;

    @Builder.Default
    private boolean allowPartialApprovals = false;

    @Builder.Default
    private boolean requireSignature = true;

    @DecimalMin(value = "0.0", message = "Signature threshold cannot be negative")
    private BigDecimal signatureThreshold;

    @Builder.Default
    private boolean requirePin = true;

    @Min(value = 5, message = "Timeout must be at least 5 seconds")
    @Max(value = 300, message = "Timeout cannot exceed 300 seconds")
    @Builder.Default
    private Integer timeoutSeconds = 30;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters (ISO 4217)")
    private String currency;
}
