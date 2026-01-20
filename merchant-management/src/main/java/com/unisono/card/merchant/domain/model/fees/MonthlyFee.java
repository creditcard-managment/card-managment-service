package com.unisono.card.merchant.domain.model.fees;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Fee mensual del merchant
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MonthlyFee {

    private BigDecimal baseAmount;
    private BigDecimal accountMaintenanceFee;
    private BigDecimal statementFee;
    private BigDecimal pciComplianceFee;
    private YearMonth billingPeriod;
    private boolean waived; // Eximido del pago

    public static MonthlyFee standard(YearMonth billingPeriod) {
        return new MonthlyFee(
                new BigDecimal("25.00"), // Base fee
                new BigDecimal("10.00"), // Account maintenance
                new BigDecimal("5.00"),  // Statement fee
                new BigDecimal("9.95"),  // PCI compliance
                billingPeriod,
                false
        );
    }

    public static MonthlyFee premium(YearMonth billingPeriod) {
        return new MonthlyFee(
                new BigDecimal("50.00"),
                new BigDecimal("15.00"),
                BigDecimal.ZERO, // Statement waived
                BigDecimal.ZERO, // PCI waived
                billingPeriod,
                false
        );
    }

    public static MonthlyFee of(
            BigDecimal baseAmount,
            BigDecimal accountMaintenanceFee,
            BigDecimal statementFee,
            BigDecimal pciComplianceFee,
            YearMonth billingPeriod,
            boolean waived
    ) {
        Objects.requireNonNull(billingPeriod, "Billing period cannot be null");

        return new MonthlyFee(
                baseAmount != null ? baseAmount : BigDecimal.ZERO,
                accountMaintenanceFee != null ? accountMaintenanceFee : BigDecimal.ZERO,
                statementFee != null ? statementFee : BigDecimal.ZERO,
                pciComplianceFee != null ? pciComplianceFee : BigDecimal.ZERO,
                billingPeriod,
                waived
        );
    }

    public BigDecimal calculateTotalFee() {
        if (waived) {
            return BigDecimal.ZERO;
        }

        return baseAmount
                .add(accountMaintenanceFee)
                .add(statementFee)
                .add(pciComplianceFee);
    }

    public void waive() {
        this.waived = true;
    }

    public void unwaive() {
        this.waived = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MonthlyFee)) return false;
        MonthlyFee that = (MonthlyFee) o;
        return Objects.equals(billingPeriod, that.billingPeriod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(billingPeriod);
    }

    @Override
    public String toString() {
        return "MonthlyFee{" +
                "period=" + billingPeriod +
                ", total=" + calculateTotalFee() +
                ", waived=" + waived +
                '}';
    }
}
