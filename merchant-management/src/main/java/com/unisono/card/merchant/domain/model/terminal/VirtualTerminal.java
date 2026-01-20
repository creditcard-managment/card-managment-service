package com.unisono.card.merchant.domain.model.terminal;

import com.unisono.card.merchant.domain.model.MerchantId;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Terminal virtual para pagos en línea
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VirtualTerminal extends PaymentTerminal {

    private String apiEndpoint;
    private String apiVersion;
    private Set<String> allowedIpAddresses;
    private boolean supports3DSecure;
    private boolean supportsTokenization;
    private boolean supportsRecurringPayments;
    private Integer maxConcurrentTransactions;

    @Builder
    private VirtualTerminal(
            TerminalId id,
            MerchantId merchantId,
            String serialNumber,
            TerminalConfiguration configuration,
            String apiEndpoint,
            String apiVersion,
            Set<String> allowedIpAddresses,
            boolean supports3DSecure,
            boolean supportsTokenization,
            boolean supportsRecurringPayments,
            Integer maxConcurrentTransactions
    ) {
        super(id, merchantId, serialNumber, configuration);
        this.apiEndpoint = apiEndpoint;
        this.apiVersion = apiVersion != null ? apiVersion : "v1";
        this.allowedIpAddresses = allowedIpAddresses;
        this.supports3DSecure = supports3DSecure;
        this.supportsTokenization = supportsTokenization;
        this.supportsRecurringPayments = supportsRecurringPayments;
        this.maxConcurrentTransactions = maxConcurrentTransactions != null ? maxConcurrentTransactions : 100;
    }

    @Override
    public TerminalType getTerminalType() {
        return TerminalType.VIRTUAL;
    }

    @Override
    public boolean supportsContactless() {
        return false; // Virtual terminals don't support contactless
    }

    @Override
    public boolean supportsChip() {
        return false; // Virtual terminals don't support chip
    }

    @Override
    public boolean supportsMagneticStripe() {
        return false; // Virtual terminals don't support magnetic stripe
    }

    public void updateApiEndpoint(String newEndpoint) {
        if (newEndpoint == null || newEndpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("API endpoint cannot be empty");
        }
        this.apiEndpoint = newEndpoint.trim();
    }

    public void addAllowedIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be empty");
        }
        this.allowedIpAddresses.add(ipAddress.trim());
    }

    public void removeAllowedIpAddress(String ipAddress) {
        this.allowedIpAddresses.remove(ipAddress);
    }

    public boolean isIpAllowed(String ipAddress) {
        return allowedIpAddresses == null ||
                allowedIpAddresses.isEmpty() ||
                allowedIpAddresses.contains(ipAddress);
    }
}
