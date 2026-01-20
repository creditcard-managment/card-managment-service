package com.unisono.card.merchant.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.unisono.card.merchant.domain.model.terminal.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO Response para Terminal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TerminalResponse {

    private String id;
    private String merchantId;
    private String serialNumber;
    private TerminalType terminalType;
    private TerminalStatus status;

    // Terminal Configuration
    private TerminalConfigurationDto configuration;

    // POS Terminal specific
    private String manufacturer;
    private String model;
    private String firmwareVersion;

    // Virtual Terminal specific
    private String apiEndpoint;
    private String apiVersion;

    // Mobile Terminal specific
    private String deviceId;
    private String deviceModel;
    private String operatingSystem;
    private String appVersion;

    // Capabilities
    private boolean supportsContactless;
    private boolean supportsChip;
    private boolean supportsMagneticStripe;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime activatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUsedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deactivatedAt;

    // Factory method genérico
    public static TerminalResponse from(PaymentTerminal terminal) {
        TerminalResponseBuilder builder = TerminalResponse.builder()
                .id(terminal.getId().getValue())
                .merchantId(terminal.getMerchantId().getValue())
                .serialNumber(terminal.getSerialNumber())
                .terminalType(terminal.getTerminalType())
                .status(terminal.getStatus())
                .configuration(TerminalConfigurationDto.from(terminal.getConfiguration()))
                .supportsContactless(terminal.supportsContactless())
                .supportsChip(terminal.supportsChip())
                .supportsMagneticStripe(terminal.supportsMagneticStripe())
                .activatedAt(terminal.getActivatedAt())
                .lastUsedAt(terminal.getLastUsedAt())
                .deactivatedAt(terminal.getDeactivatedAt());

        // Agregar campos específicos según el tipo
        if (terminal instanceof POSTerminal pos) {
            builder.manufacturer(pos.getManufacturer())
                    .model(pos.getModel())
                    .firmwareVersion(pos.getFirmwareVersion());
        } else if (terminal instanceof VirtualTerminal virtual) {
            builder.apiEndpoint(virtual.getApiEndpoint())
                    .apiVersion(virtual.getApiVersion());
        } else if (terminal instanceof MobileTerminal mobile) {
            builder.deviceId(mobile.getDeviceId())
                    .deviceModel(mobile.getDeviceModel())
                    .operatingSystem(mobile.getOperatingSystem())
                    .appVersion(mobile.getAppVersion());
        }

        return builder.build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TerminalConfigurationDto {
        private java.util.Set<String> acceptedCardBrands;
        private java.math.BigDecimal maxTransactionAmount;
        private java.math.BigDecimal minTransactionAmount;
        private boolean allowRefunds;
        private boolean allowVoids;
        private boolean requireSignature;
        private java.math.BigDecimal signatureThreshold;
        private boolean requirePin;
        private Integer timeoutSeconds;
        private String currency;

        public static TerminalConfigurationDto from(TerminalConfiguration config) {
            return TerminalConfigurationDto.builder()
                    .acceptedCardBrands(config.getAcceptedCardBrands())
                    .maxTransactionAmount(config.getMaxTransactionAmount())
                    .minTransactionAmount(config.getMinTransactionAmount())
                    .allowRefunds(config.isAllowRefunds())
                    .allowVoids(config.isAllowVoids())
                    .requireSignature(config.isRequireSignature())
                    .signatureThreshold(config.getSignatureThreshold())
                    .requirePin(config.isRequirePin())
                    .timeoutSeconds(config.getTimeoutSeconds())
                    .currency(config.getCurrency())
                    .build();
        }
    }
}
