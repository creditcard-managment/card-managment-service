package com.unisono.card.merchant.domain.model.fees;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Acquirer Fee (fee cobrado por el banco adquirente/procesador)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AcquirerFee {

    private BigDecimal processingRate; // Tasa de procesamiento (%)
    private BigDecimal authorizationFee; // Fee por autorización
    private BigDecimal settlementFee; // Fee por liquidación
    private BigDecimal monthlyMinimum; // Mínimo mensual

    public static AcquirerFee standard() {
        return new AcquirerFee(
                new BigDecimal("0.25"), // 0.25% processing
                new BigDecimal("0.10"), // $0.10 por autorización
                new BigDecimal("0.05"), // $0.05 por settlement
                BigDecimal.ZERO
        );
    }

    public static AcquirerFee premium() {
        return new AcquirerFee(
                new BigDecimal("0.15"), // 0.15% processing (mejor rate)
                new BigDecimal("0.08"),
                new BigDecimal("0.03"),
                BigDecimal.ZERO
        );
    }

    public static AcquirerFee of(
            BigDecimal processingRate,
            BigDecimal authorizationFee,
            BigDecimal settlementFee,
            BigDecimal monthlyMinimum
    ) {
        Objects.requireNonNull(processingRate, "Processing rate cannot be null");
        validateRate(processingRate);

        return new AcquirerFee(
                processingRate,
                authorizationFee != null ? authorizationFee : BigDecimal.ZERO,
                settlementFee != null ? settlementFee : BigDecimal.ZERO,
                monthlyMinimum != null ? monthlyMinimum : BigDecimal.ZERO
        );
    }

    private static void validateRate(BigDecimal rate) {
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(new BigDecimal("5")) > 0) {
            throw new IllegalArgumentException("Processing rate must be between 0 and 5%");
        }
    }

    public BigDecimal calculateFee(BigDecimal transactionAmount) {
        Objects.requireNonNull(transactionAmount, "Transaction amount cannot be null");

        BigDecimal processingFee = transactionAmount
                .multiply(processingRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        return processingFee
                .add(authorizationFee)
                .add(settlementFee)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateMonthlyFee(BigDecimal totalProcessed, Integer transactionCount) {
        BigDecimal calculatedFees = BigDecimal.ZERO;

        if (transactionCount != null && transactionCount > 0) {
            BigDecimal authFees = authorizationFee.multiply(new BigDecimal(transactionCount));
            BigDecimal settleFees = settlementFee.multiply(new BigDecimal(transactionCount));
            calculatedFees = authFees.add(settleFees);
        }

        // Aplicar mínimo mensual si corresponde
        if (monthlyMinimum.compareTo(BigDecimal.ZERO) > 0 && calculatedFees.compareTo(monthlyMinimum) < 0) {
            return monthlyMinimum;
        }

        return calculatedFees;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AcquirerFee)) return false;
        AcquirerFee that = (AcquirerFee) o;
        return Objects.equals(processingRate, that.processingRate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processingRate);
    }

    @Override
    public String toString() {
        return "AcquirerFee{" +
                "processingRate=" + processingRate +
                "%, authFee=" + authorizationFee +
                ", settlementFee=" + settlementFee +
                '}';
    }
}
