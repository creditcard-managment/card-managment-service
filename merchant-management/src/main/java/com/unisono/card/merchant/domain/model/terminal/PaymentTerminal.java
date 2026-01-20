package com.unisono.card.merchant.domain.model.terminal;

import com.unisono.card.merchant.domain.model.MerchantId;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Clase abstracta base para todas las terminales de pago
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PaymentTerminal {

    protected TerminalId id;
    protected MerchantId merchantId;
    protected String serialNumber;
    protected TerminalStatus status;
    protected TerminalConfiguration configuration;
    protected LocalDateTime activatedAt;
    protected LocalDateTime lastUsedAt;
    protected LocalDateTime deactivatedAt;
    protected Integer version;

    protected PaymentTerminal(
            TerminalId id,
            MerchantId merchantId,
            String serialNumber,
            TerminalConfiguration configuration
    ) {
        this.id = Objects.requireNonNull(id, "TerminalId cannot be null");
        this.merchantId = Objects.requireNonNull(merchantId, "MerchantId cannot be null");
        this.serialNumber = validateSerialNumber(serialNumber);
        this.configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");
        this.status = TerminalStatus.INACTIVE;
    }

    private String validateSerialNumber(String serialNumber) {
        Objects.requireNonNull(serialNumber, "Serial number cannot be null");
        String cleaned = serialNumber.trim();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Serial number cannot be empty");
        }
        if (cleaned.length() < 8 || cleaned.length() > 50) {
            throw new IllegalArgumentException("Serial number must be between 8 and 50 characters");
        }
        return cleaned;
    }

    public void activate() {
        if (this.status == TerminalStatus.ACTIVE) {
            throw new IllegalStateException("Terminal is already active");
        }

        if (this.status == TerminalStatus.DECOMMISSIONED) {
            throw new IllegalStateException("Cannot activate a decommissioned terminal");
        }

        this.status = TerminalStatus.ACTIVE;
        this.activatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        if (this.status == TerminalStatus.INACTIVE) {
            throw new IllegalStateException("Terminal is already inactive");
        }

        this.status = TerminalStatus.INACTIVE;
        this.deactivatedAt = LocalDateTime.now();
    }

    public void suspend() {
        if (this.status != TerminalStatus.ACTIVE) {
            throw new IllegalStateException("Only active terminals can be suspended");
        }

        this.status = TerminalStatus.SUSPENDED;
    }

    public void decommission() {
        this.status = TerminalStatus.DECOMMISSIONED;
        this.deactivatedAt = LocalDateTime.now();
    }

    public void updateConfiguration(TerminalConfiguration newConfiguration) {
        this.configuration = Objects.requireNonNull(newConfiguration, "Configuration cannot be null");
    }

    public void recordUsage() {
        this.lastUsedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.status == TerminalStatus.ACTIVE;
    }

    public boolean canProcessTransactions() {
        return this.status == TerminalStatus.ACTIVE;
    }

    public abstract TerminalType getTerminalType();
    public abstract boolean supportsContactless();
    public abstract boolean supportsChip();
    public abstract boolean supportsMagneticStripe();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaymentTerminal)) return false;
        PaymentTerminal that = (PaymentTerminal) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", serialNumber='" + serialNumber + '\'' +
                ", status=" + status +
                '}';
    }
}
