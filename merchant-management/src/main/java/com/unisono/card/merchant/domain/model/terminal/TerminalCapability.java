package com.unisono.card.merchant.domain.model.terminal;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Capacidades técnicas de una terminal
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TerminalCapability {

    private boolean supportsContactless;
    private boolean supportsChip;
    private boolean supportsMagneticStripe;
    private boolean supportsManualEntry;
    private boolean supportsPinEntry;
    private boolean supportsSignatureCapture;
    private boolean supportsPrinting;
    private boolean supportsOfflineTransactions;
    private boolean supports3DSecure;
    private Integer maxOfflineTransactions;

    public static TerminalCapability fullCapability() {
        return new TerminalCapability(
                true, true, true, true, true, true, true, true, true, 10
        );
    }

    public static TerminalCapability basicCapability() {
        return new TerminalCapability(
                false, true, true, true, true, false, false, false, false, 0
        );
    }

    public static TerminalCapability virtualCapability() {
        return new TerminalCapability(
                false, false, false, true, false, false, false, false, true, 0
        );
    }

    public static TerminalCapability of(
            boolean supportsContactless,
            boolean supportsChip,
            boolean supportsMagneticStripe,
            boolean supportsManualEntry,
            boolean supportsPinEntry,
            boolean supportsSignatureCapture,
            boolean supportsPrinting,
            boolean supportsOfflineTransactions,
            boolean supports3DSecure,
            Integer maxOfflineTransactions
    ) {
        return new TerminalCapability(
                supportsContactless,
                supportsChip,
                supportsMagneticStripe,
                supportsManualEntry,
                supportsPinEntry,
                supportsSignatureCapture,
                supportsPrinting,
                supportsOfflineTransactions,
                supports3DSecure,
                maxOfflineTransactions != null ? maxOfflineTransactions : 0
        );
    }

    public boolean canReadCard() {
        return supportsContactless || supportsChip || supportsMagneticStripe;
    }

    public boolean isEMVCompliant() {
        return supportsChip;
    }

    public boolean isPCICompliant() {
        return supportsPinEntry || supportsChip;
    }

    public boolean canProcessOffline() {
        return supportsOfflineTransactions && maxOfflineTransactions > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TerminalCapability)) return false;
        TerminalCapability that = (TerminalCapability) o;
        return supportsContactless == that.supportsContactless &&
                supportsChip == that.supportsChip &&
                supportsMagneticStripe == that.supportsMagneticStripe;
    }

    @Override
    public int hashCode() {
        return Objects.hash(supportsContactless, supportsChip, supportsMagneticStripe);
    }

    @Override
    public String toString() {
        return "TerminalCapability{" +
                "contactless=" + supportsContactless +
                ", chip=" + supportsChip +
                ", stripe=" + supportsMagneticStripe +
                ", 3DS=" + supports3DSecure +
                '}';
    }
}
