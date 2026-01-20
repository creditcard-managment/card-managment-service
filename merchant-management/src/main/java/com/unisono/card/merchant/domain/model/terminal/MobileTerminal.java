package com.unisono.card.merchant.domain.model.terminal;

import com.unisono.card.merchant.domain.model.MerchantId;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Terminal móvil (smartphone/tablet con lector)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MobileTerminal extends PaymentTerminal {

    private String deviceId;
    private String deviceModel;
    private String operatingSystem;
    private String appVersion;
    private boolean hasExternalReader;
    private boolean supportsBluetooth;
    private boolean supportsNFC;
    private String readerSerialNumber;

    @Builder
    private MobileTerminal(
            TerminalId id,
            MerchantId merchantId,
            String serialNumber,
            TerminalConfiguration configuration,
            String deviceId,
            String deviceModel,
            String operatingSystem,
            String appVersion,
            boolean hasExternalReader,
            boolean supportsBluetooth,
            boolean supportsNFC,
            String readerSerialNumber
    ) {
        super(id, merchantId, serialNumber, configuration);
        this.deviceId = deviceId;
        this.deviceModel = deviceModel;
        this.operatingSystem = operatingSystem;
        this.appVersion = appVersion;
        this.hasExternalReader = hasExternalReader;
        this.supportsBluetooth = supportsBluetooth;
        this.supportsNFC = supportsNFC;
        this.readerSerialNumber = readerSerialNumber;
    }

    @Override
    public TerminalType getTerminalType() {
        return TerminalType.MOBILE;
    }

    @Override
    public boolean supportsContactless() {
        return supportsNFC || (hasExternalReader && supportsBluetooth);
    }

    @Override
    public boolean supportsChip() {
        return hasExternalReader;
    }

    @Override
    public boolean supportsMagneticStripe() {
        return hasExternalReader;
    }

    public void updateAppVersion(String newVersion) {
        if (newVersion == null || newVersion.trim().isEmpty()) {
            throw new IllegalArgumentException("App version cannot be empty");
        }
        this.appVersion = newVersion.trim();
    }

    public boolean requiresAppUpdate(String latestVersion) {
        return !this.appVersion.equals(latestVersion);
    }
}
