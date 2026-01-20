package com.unisono.card.merchant.application.dto.request;

import com.unisono.card.merchant.domain.model.terminal.TerminalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO Request para aprovisionar una terminal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvisionTerminalRequest {

    @NotBlank(message = "Merchant ID is required")
    private String merchantId;

    @NotNull(message = "Terminal type is required")
    private TerminalType terminalType;

    @NotBlank(message = "Serial number is required")
    @Size(min = 8, max = 50, message = "Serial number must be between 8 and 50 characters")
    private String serialNumber;

    // POS Terminal specific fields
    private String manufacturer;
    private String model;
    private String firmwareVersion;

    // Virtual Terminal specific fields
    private String apiEndpoint;
    private String apiVersion;

    // Mobile Terminal specific fields
    private String deviceId;
    private String deviceModel;
    private String operatingSystem;
    private String appVersion;

    // Common configuration
    @Builder.Default
    private boolean activateImmediately = false;

    @Builder.Default
    private boolean requireSignature = true;

    @Builder.Default
    private boolean requirePin = true;
}
