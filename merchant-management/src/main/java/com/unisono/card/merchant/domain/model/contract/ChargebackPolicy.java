package com.unisono.card.merchant.domain.model.contract;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Política de chargebacks del contrato
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChargebackPolicy {

    private BigDecimal chargebackFee; // Fee por cada chargeback
    private BigDecimal maxChargebackRatio; // Ratio máximo permitido (%)
    private Integer warningThresholdCount; // Cantidad de chargebacks para warning
    private BigDecimal warningThresholdRatio; // Ratio de chargebacks para warning (%)
    private Integer reviewPeriodDays; // Período de revisión en días
    private boolean automaticSuspensionEnabled; // Suspensión automática al exceder límites
    private String penaltyStructure; // Estructura de penalidades

    public static ChargebackPolicy standard() {
        return new ChargebackPolicy(
                new BigDecimal("15.00"), // $15 por chargeback
                new BigDecimal("1.0"), // Máximo 1% de chargebacks
                10, // Warning a los 10 chargebacks
                new BigDecimal("0.75"), // Warning al 0.75%
                30, // Revisión mensual
                false,
                "Standard penalty structure"
        );
    }

    public static ChargebackPolicy highRisk() {
        return new ChargebackPolicy(
                new BigDecimal("25.00"),
                new BigDecimal("0.5"), // Más restrictivo
                5,
                new BigDecimal("0.4"),
                30,
                true, // Suspensión automática
                "High-risk penalty structure"
        );
    }

    public static ChargebackPolicy of(
            BigDecimal chargebackFee,
            BigDecimal maxChargebackRatio,
            Integer warningThresholdCount,
            BigDecimal warningThresholdRatio,
            Integer reviewPeriodDays,
            boolean automaticSuspensionEnabled,
            String penaltyStructure
    ) {
        validateFee(chargebackFee);
        validateRatio(maxChargebackRatio, "Max chargeback ratio");
        validateRatio(warningThresholdRatio, "Warning threshold ratio");
        Objects.requireNonNull(warningThresholdCount, "Warning threshold count cannot be null");
        Objects.requireNonNull(reviewPeriodDays, "Review period days cannot be null");

        return new ChargebackPolicy(
                chargebackFee,
                maxChargebackRatio,
                warningThresholdCount,
                warningThresholdRatio,
                reviewPeriodDays,
                automaticSuspensionEnabled,
                penaltyStructure != null ? penaltyStructure : "Standard penalty structure"
        );
    }

    private static void validateFee(BigDecimal fee) {
        Objects.requireNonNull(fee, "Chargeback fee cannot be null");
        if (fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Chargeback fee cannot be negative");
        }
    }

    private static void validateRatio(BigDecimal ratio, String fieldName) {
        Objects.requireNonNull(ratio, fieldName + " cannot be null");
        if (ratio.compareTo(BigDecimal.ZERO) < 0 || ratio.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 100");
        }
    }

    public boolean isChargebackRatioExceeded(BigDecimal currentRatio) {
        return currentRatio.compareTo(maxChargebackRatio) > 0;
    }

    public boolean isWarningThresholdExceeded(BigDecimal currentRatio, Integer chargebackCount) {
        return currentRatio.compareTo(warningThresholdRatio) > 0 ||
                chargebackCount >= warningThresholdCount;
    }

    public BigDecimal calculatePenalty(Integer chargebackCount) {
        return chargebackFee.multiply(new BigDecimal(chargebackCount));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChargebackPolicy)) return false;
        ChargebackPolicy that = (ChargebackPolicy) o;
        return Objects.equals(maxChargebackRatio, that.maxChargebackRatio) &&
                Objects.equals(reviewPeriodDays, that.reviewPeriodDays);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxChargebackRatio, reviewPeriodDays);
    }

    @Override
    public String toString() {
        return "ChargebackPolicy{" +
                "chargebackFee=" + chargebackFee +
                ", maxChargebackRatio=" + maxChargebackRatio +
                ", automaticSuspension=" + automaticSuspensionEnabled +
                '}';
    }
}
