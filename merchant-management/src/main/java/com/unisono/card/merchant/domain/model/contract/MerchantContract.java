package com.unisono.card.merchant.domain.model.contract;

import com.unisono.card.merchant.domain.model.fees.MerchantFeeStructure;
import lombok.*;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * Contrato entre el Merchant y el banco/adquirente
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MerchantContract {

    private ContractId id;
    private LocalDate startDate;
    private LocalDate endDate;
    private ContractTerms terms;
    private MerchantFeeStructure feeStructure;
    private ChargebackPolicy chargebackPolicy;
    private ServiceLevel serviceLevel;
    private boolean autoRenew;
    private Integer renewalPeriodMonths;
    private ContractStatus status;

    @Builder
    private MerchantContract(
            ContractId id,
            LocalDate startDate,
            LocalDate endDate,
            ContractTerms terms,
            MerchantFeeStructure feeStructure,
            ChargebackPolicy chargebackPolicy,
            ServiceLevel serviceLevel,
            boolean autoRenew,
            Integer renewalPeriodMonths,
            ContractStatus status
    ) {
        this.id = Objects.requireNonNull(id, "ContractId cannot be null");
        this.startDate = Objects.requireNonNull(startDate, "Start date cannot be null");
        this.endDate = Objects.requireNonNull(endDate, "End date cannot be null");

        validateDates(startDate, endDate);

        this.terms = Objects.requireNonNull(terms, "Contract terms cannot be null");
        this.feeStructure = Objects.requireNonNull(feeStructure, "Fee structure cannot be null");
        this.chargebackPolicy = Objects.requireNonNull(chargebackPolicy, "Chargeback policy cannot be null");
        this.serviceLevel = Objects.requireNonNull(serviceLevel, "Service level cannot be null");
        this.autoRenew = autoRenew;
        this.renewalPeriodMonths = renewalPeriodMonths != null ? renewalPeriodMonths : 12;
        this.status = ContractStatus.ACTIVE;
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        if (end.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("End date cannot be in the past");
        }
    }

    public void activate() {
        if (this.status != ContractStatus.DRAFT) {
            throw new IllegalStateException("Only draft contracts can be activated");
        }
        this.status = ContractStatus.ACTIVE;
    }

    public void suspend() {
        if (this.status != ContractStatus.ACTIVE) {
            throw new IllegalStateException("Only active contracts can be suspended");
        }
        this.status = ContractStatus.SUSPENDED;
    }

    public void terminate() {
        this.status = ContractStatus.TERMINATED;
    }

    public void renew(LocalDate newEndDate) {
        if (!autoRenew) {
            throw new IllegalStateException("Contract is not set for auto-renewal");
        }

        if (newEndDate.isBefore(this.endDate)) {
            throw new IllegalArgumentException("New end date must be after current end date");
        }

        this.endDate = newEndDate;
        this.status = ContractStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == ContractStatus.ACTIVE &&
                !LocalDate.now().isAfter(endDate);
    }

    public boolean isExpiringSoon(int daysThreshold) {
        LocalDate threshold = LocalDate.now().plusDays(daysThreshold);
        return endDate.isBefore(threshold);
    }

    public Period getRemainingPeriod() {
        return Period.between(LocalDate.now(), endDate);
    }

    public long getDaysRemaining() {
        return Period.between(LocalDate.now(), endDate).getDays();
    }

    /**
     * Actualiza la estructura de fees del contrato
     */
    public void updateFeeStructure(MerchantFeeStructure newFeeStructure) {
        Objects.requireNonNull(newFeeStructure, "Fee structure cannot be null");

        if (!this.isActive()) {
            throw new IllegalStateException("Cannot update fee structure of inactive contract");
        }

        // Desactivar la estructura anterior si existe
        if (this.feeStructure != null) {
            this.feeStructure.deactivate();
        }

        this.feeStructure = newFeeStructure;
    }

    /**
     * Actualiza la configuración de auto-renovación
     */
    public void updateAutoRenew(boolean autoRenew, Integer renewalPeriodMonths) {
        this.autoRenew = autoRenew;
        if (renewalPeriodMonths != null && renewalPeriodMonths > 0) {
            this.renewalPeriodMonths = renewalPeriodMonths;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MerchantContract)) return false;
        MerchantContract that = (MerchantContract) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MerchantContract{" +
                "id=" + id +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status=" + status +
                '}';
    }
}
