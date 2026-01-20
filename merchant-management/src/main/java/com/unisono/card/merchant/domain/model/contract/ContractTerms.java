package com.unisono.card.merchant.domain.model.contract;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Términos y condiciones del contrato
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContractTerms {

    private Integer settlementDays; // Días para liquidación
    private BigDecimal minimumMonthlyVolume; // Volumen mínimo mensual
    private BigDecimal maximumMonthlyVolume; // Volumen máximo mensual
    private BigDecimal reservePercentage; // Porcentaje de reserva
    private Integer earlyTerminationNoticeDays; // Días de aviso para terminación anticipada
    private String legalJurisdiction; // Jurisdicción legal
    private String disputeResolutionMethod; // Método de resolución de disputas

    public static ContractTerms standard() {
        return new ContractTerms(
                2, // T+2 settlement
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                30,
                "Argentina",
                "Arbitration"
        );
    }

    public static ContractTerms of(
            Integer settlementDays,
            BigDecimal minimumMonthlyVolume,
            BigDecimal maximumMonthlyVolume,
            BigDecimal reservePercentage,
            Integer earlyTerminationNoticeDays,
            String legalJurisdiction,
            String disputeResolutionMethod
    ) {
        validateSettlementDays(settlementDays);
        validateReservePercentage(reservePercentage);
        validateVolumes(minimumMonthlyVolume, maximumMonthlyVolume);
        Objects.requireNonNull(legalJurisdiction, "Legal jurisdiction cannot be null");

        return new ContractTerms(
                settlementDays,
                minimumMonthlyVolume != null ? minimumMonthlyVolume : BigDecimal.ZERO,
                maximumMonthlyVolume,
                reservePercentage != null ? reservePercentage : BigDecimal.ZERO,
                earlyTerminationNoticeDays != null ? earlyTerminationNoticeDays : 30,
                legalJurisdiction.trim(),
                disputeResolutionMethod != null ? disputeResolutionMethod.trim() : "Arbitration"
        );
    }

    private static void validateSettlementDays(Integer days) {
        Objects.requireNonNull(days, "Settlement days cannot be null");
        if (days < 1 || days > 30) {
            throw new IllegalArgumentException("Settlement days must be between 1 and 30");
        }
    }

    private static void validateReservePercentage(BigDecimal percentage) {
        if (percentage != null) {
            if (percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Reserve percentage must be between 0 and 100");
            }
        }
    }

    private static void validateVolumes(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("Minimum volume cannot exceed maximum volume");
        }
    }

    public boolean hasVolumeRestrictions() {
        return minimumMonthlyVolume.compareTo(BigDecimal.ZERO) > 0 || maximumMonthlyVolume != null;
    }

    public boolean hasReserveRequirement() {
        return reservePercentage.compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContractTerms)) return false;
        ContractTerms that = (ContractTerms) o;
        return Objects.equals(settlementDays, that.settlementDays) &&
                Objects.equals(legalJurisdiction, that.legalJurisdiction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(settlementDays, legalJurisdiction);
    }

    @Override
    public String toString() {
        return "ContractTerms{" +
                "settlementDays=" + settlementDays +
                ", reservePercentage=" + reservePercentage +
                ", jurisdiction='" + legalJurisdiction + '\'' +
                '}';
    }
}