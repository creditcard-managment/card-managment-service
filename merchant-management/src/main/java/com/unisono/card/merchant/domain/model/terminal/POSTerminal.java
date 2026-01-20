package com.unisono.card.merchant.domain.model.terminal;

import com.unisono.card.merchant.domain.model.MerchantId;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Terminal de Punto de Venta (POS) física
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class POSTerminal extends PaymentTerminal {

    private String manufacturer;
    private String model;
    private String firmwareVersion;
    private boolean hasContactlessReader;
    private boolean hasChipReader;
    private boolean hasMagneticStripeReader;
    private boolean hasPinPad;
    private boolean hasPrinter;

    @Builder
    private POSTerminal(
            TerminalId id,
            MerchantId merchantId,
            String serialNumber,
            TerminalConfiguration configuration,
            String manufacturer,
            String model,
            String firmwareVersion,
            boolean hasContactlessReader,
            boolean hasChipReader,
            boolean hasMagneticStripeReader,
            boolean hasPinPad,
            boolean hasPrinter
    ) {
        super(id, merchantId, serialNumber, configuration);
        this.manufacturer = manufacturer != null ? manufacturer : "Unknown";
        this.model = model != null ? model : "Unknown";
        this.firmwareVersion = firmwareVersion;
        this.hasContactlessReader = hasContactlessReader;
        this.hasChipReader = hasChipReader;
        this.hasMagneticStripeReader = hasMagneticStripeReader;
        this.hasPinPad = hasPinPad;
        this.hasPrinter = hasPrinter;
    }

    @Override
    public TerminalType getTerminalType() {
        return TerminalType.POS;
    }

    @Override
    public boolean supportsContactless() {
        return hasContactlessReader;
    }

    @Override
    public boolean supportsChip() {
        return hasChipReader;
    }

    @Override
    public boolean supportsMagneticStripe() {
        return hasMagneticStripeReader;
    }

    public void updateFirmware(String newVersion) {
        if (newVersion == null || newVersion.trim().isEmpty()) {
            throw new IllegalArgumentException("Firmware version cannot be empty");
        }
        this.firmwareVersion = newVersion.trim();
    }

    public boolean requiresMaintenanceCheck() {
        if (lastUsedAt == null) {
            return false;
        }
        // Requiere mantenimiento si no se usa por más de 90 días
        return lastUsedAt.plusDays(90).isBefore(java.time.LocalDateTime.now());
    }
}
