package com.unisono.card.merchant.domain.model;

import com.unisono.card.merchant.domain.model.contract.MerchantContract;
import com.unisono.card.merchant.domain.model.terminal.PaymentTerminal;
import com.unisono.card.merchant.domain.model.terminal.TerminalId;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Merchant Aggregate Root
 * Representa un comercio que acepta pagos con tarjeta de crédito
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Merchant {

    private MerchantId id;
    private MerchantProfile profile;
    private BusinessInformation businessInformation;
    private TaxInformation taxInformation;
    private MerchantCategory category;
    private MerchantType type;
    private MerchantStatus status;
    private MerchantContract contract;

    @Setter
    private List<PaymentTerminal> terminals = new ArrayList<>();

    private LocalDateTime registeredAt;
    private LocalDateTime activatedAt;
    private LocalDateTime lastUpdatedAt;

    private Integer version; // Optimistic locking

    @Builder
    private Merchant(
            MerchantId id,
            MerchantProfile profile,
            BusinessInformation businessInformation,
            TaxInformation taxInformation,
            MerchantCategory category,
            MerchantType type,
            MerchantContract contract
    ) {
        this.id = Objects.requireNonNull(id, "MerchantId cannot be null");
        this.profile = Objects.requireNonNull(profile, "Profile cannot be null");
        this.businessInformation = Objects.requireNonNull(businessInformation, "BusinessInformation cannot be null");
        this.taxInformation = Objects.requireNonNull(taxInformation, "TaxInformation cannot be null");
        this.category = Objects.requireNonNull(category, "Category cannot be null");
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        this.contract = contract;
        this.status = MerchantStatus.PENDING_APPROVAL;
        this.registeredAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
        this.terminals = new ArrayList<>();
    }

    // ============================================
    // BUSINESS METHODS
    // ============================================

    public void activate() {
        if (this.status == MerchantStatus.ACTIVE) {
            throw new IllegalStateException("Merchant is already active");
        }

        if (this.status == MerchantStatus.CLOSED) {
            throw new IllegalStateException("Cannot activate a closed merchant");
        }

        this.status = MerchantStatus.ACTIVE;
        this.activatedAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public void suspend(String reason) {
        if (this.status != MerchantStatus.ACTIVE) {
            throw new IllegalStateException("Only active merchants can be suspended");
        }

        Objects.requireNonNull(reason, "Suspension reason cannot be null");

        this.status = MerchantStatus.SUSPENDED;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public void close() {
        this.status = MerchantStatus.CLOSED;
        this.lastUpdatedAt = LocalDateTime.now();

        // Desactivar todas las terminales
        this.terminals.forEach(PaymentTerminal::deactivate);
    }

    public void updateProfile(MerchantProfile newProfile) {
        this.profile = Objects.requireNonNull(newProfile, "Profile cannot be null");
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public void updateBusinessInformation(BusinessInformation newInfo) {
        this.businessInformation = Objects.requireNonNull(newInfo, "BusinessInformation cannot be null");
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public void addTerminal(PaymentTerminal terminal) {
        Objects.requireNonNull(terminal, "Terminal cannot be null");

        if (this.status != MerchantStatus.ACTIVE) {
            throw new IllegalStateException("Cannot add terminal to non-active merchant");
        }

        this.terminals.add(terminal);
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public void removeTerminal(TerminalId terminalId) {
        boolean removed = this.terminals.removeIf(t -> t.getId().equals(terminalId));

        if (!removed) {
            throw new IllegalArgumentException("Terminal not found: " + terminalId);
        }

        this.lastUpdatedAt = LocalDateTime.now();
    }

    public void renewContract(MerchantContract newContract) {
        this.contract = Objects.requireNonNull(newContract, "Contract cannot be null");
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.status == MerchantStatus.ACTIVE;
    }

    public boolean canProcessTransactions() {
        return this.status == MerchantStatus.ACTIVE && hasActiveTerminals();
    }

    public boolean hasActiveTerminals() {
        return this.terminals.stream()
                .anyMatch(PaymentTerminal::isActive);
    }

    public List<PaymentTerminal> getTerminals() {
        return Collections.unmodifiableList(terminals);
    }

    public int getActiveTerminalsCount() {
        return (int) terminals.stream()
                .filter(PaymentTerminal::isActive)
                .count();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Merchant)) return false;
        Merchant merchant = (Merchant) o;
        return Objects.equals(id, merchant.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Merchant{" +
                "id=" + id +
                ", legalName='" + businessInformation.getLegalName() + '\'' +
                ", category=" + category +
                ", type=" + type +
                ", status=" + status +
                '}';
    }
}