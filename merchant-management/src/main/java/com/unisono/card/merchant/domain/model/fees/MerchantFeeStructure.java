package com.unisono.card.merchant.domain.model.fees;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Estructura de comisiones del merchant
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MerchantFeeStructure {

    private FeeStructureId id;
    private List<TransactionFee> transactionFees;
    private InterchangeFee interchangeFee;
    private AcquirerFee acquirerFee;
    private BigDecimal monthlyFee;
    private BigDecimal setupFee;
    private BigDecimal chargebackFee;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private boolean isActive;

    @Builder
    private MerchantFeeStructure(
            FeeStructureId id,
            List<TransactionFee> transactionFees,
            InterchangeFee interchangeFee,
            AcquirerFee acquirerFee,
            BigDecimal monthlyFee,
            BigDecimal setupFee,
            BigDecimal chargebackFee,
            LocalDateTime effectiveFrom
    ) {
        this.id = Objects.requireNonNull(id, "FeeStructureId cannot be null");
        this.transactionFees = transactionFees != null ? new ArrayList<>(transactionFees) : new ArrayList<>();
        this.interchangeFee = Objects.requireNonNull(interchangeFee, "Interchange fee cannot be null");
        this.acquirerFee = Objects.requireNonNull(acquirerFee, "Acquirer fee cannot be null");
        this.monthlyFee = monthlyFee != null ? monthlyFee : BigDecimal.ZERO;
        this.setupFee = setupFee != null ? setupFee : BigDecimal.ZERO;
        this.chargebackFee = chargebackFee != null ? chargebackFee : new BigDecimal("15.00");
        this.effectiveFrom = effectiveFrom != null ? effectiveFrom : LocalDateTime.now();
        this.effectiveTo = null;
        this.isActive = true;
    }

    public void addTransactionFee(TransactionFee fee) {
        Objects.requireNonNull(fee, "Transaction fee cannot be null");
        this.transactionFees.add(fee);
    }

    public void removeTransactionFee(TransactionFee fee) {
        this.transactionFees.remove(fee);
    }

    public void deactivate() {
        this.isActive = false;
        this.effectiveTo = LocalDateTime.now();
    }

    public BigDecimal calculateTotalFee(BigDecimal transactionAmount, String cardBrand, String transactionType) {
        if (!isActive) {
            throw new IllegalStateException("Fee structure is not active");
        }

        BigDecimal total = BigDecimal.ZERO;

        // Transaction fee
        TransactionFee applicableFee = findApplicableTransactionFee(cardBrand, transactionType);
        if (applicableFee != null) {
            total = total.add(applicableFee.calculateFee(transactionAmount));
        }

        // Interchange fee
        total = total.add(interchangeFee.calculateFee(transactionAmount));

        // Acquirer fee
        total = total.add(acquirerFee.calculateFee(transactionAmount));

        return total;
    }

    private TransactionFee findApplicableTransactionFee(String cardBrand, String transactionType) {
        return transactionFees.stream()
                .filter(fee -> fee.isApplicable(cardBrand, transactionType))
                .findFirst()
                .orElse(null);
    }

    public List<TransactionFee> getTransactionFees() {
        return Collections.unmodifiableList(transactionFees);
    }

    public boolean isCurrentlyActive() {
        if (!isActive) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        boolean afterStart = effectiveFrom == null || !now.isBefore(effectiveFrom);
        boolean beforeEnd = effectiveTo == null || !now.isAfter(effectiveTo);

        return afterStart && beforeEnd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MerchantFeeStructure)) return false;
        MerchantFeeStructure that = (MerchantFeeStructure) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MerchantFeeStructure{" +
                "id=" + id +
                ", monthlyFee=" + monthlyFee +
                ", isActive=" + isActive +
                '}';
    }
}
