package com.unisono.card.merchant.domain.model.contract;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Service Level Agreement (SLA) del contrato
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceLevel {

    private ServiceTier tier;
    private BigDecimal uptimeGuarantee; // Porcentaje de uptime garantizado
    private Integer supportResponseTimeMinutes; // Tiempo de respuesta de soporte
    private Integer disputeResolutionDays; // Días para resolver disputas
    private boolean dedicatedAccountManager; // Gerente de cuenta dedicado
    private boolean prioritySupport; // Soporte prioritario
    private Integer maxTransactionsPerDay; // Máximo de transacciones por día
    private BigDecimal maxTransactionAmount; // Monto máximo por transacción

    public static ServiceLevel basic() {
        return new ServiceLevel(
                ServiceTier.BASIC,
                new BigDecimal("99.0"), // 99% uptime
                240, // 4 horas de respuesta
                10, // 10 días para disputas
                false,
                false,
                1000,
                new BigDecimal("10000.00")
        );
    }

    public static ServiceLevel premium() {
        return new ServiceLevel(
                ServiceTier.PREMIUM,
                new BigDecimal("99.9"), // 99.9% uptime
                60, // 1 hora de respuesta
                5, // 5 días para disputas
                true,
                true,
                10000,
                new BigDecimal("100000.00")
        );
    }

    public static ServiceLevel enterprise() {
        return new ServiceLevel(
                ServiceTier.ENTERPRISE,
                new BigDecimal("99.99"), // 99.99% uptime
                15, // 15 minutos de respuesta
                3, // 3 días para disputas
                true,
                true,
                null, // Sin límite
                null  // Sin límite
        );
    }

    public static ServiceLevel of(
            ServiceTier tier,
            BigDecimal uptimeGuarantee,
            Integer supportResponseTimeMinutes,
            Integer disputeResolutionDays,
            boolean dedicatedAccountManager,
            boolean prioritySupport,
            Integer maxTransactionsPerDay,
            BigDecimal maxTransactionAmount
    ) {
        Objects.requireNonNull(tier, "Service tier cannot be null");
        validateUptime(uptimeGuarantee);
        Objects.requireNonNull(supportResponseTimeMinutes, "Support response time cannot be null");
        Objects.requireNonNull(disputeResolutionDays, "Dispute resolution days cannot be null");

        return new ServiceLevel(
                tier,
                uptimeGuarantee,
                supportResponseTimeMinutes,
                disputeResolutionDays,
                dedicatedAccountManager,
                prioritySupport,
                maxTransactionsPerDay,
                maxTransactionAmount
        );
    }

    private static void validateUptime(BigDecimal uptime) {
        Objects.requireNonNull(uptime, "Uptime guarantee cannot be null");
        if (uptime.compareTo(new BigDecimal("90")) < 0 || uptime.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Uptime guarantee must be between 90 and 100");
        }
    }

    public boolean hasTransactionLimits() {
        return maxTransactionsPerDay != null || maxTransactionAmount != null;
    }

    public boolean isTransactionAllowed(Integer dailyCount, BigDecimal amount) {
        boolean countOk = maxTransactionsPerDay == null || dailyCount < maxTransactionsPerDay;
        boolean amountOk = maxTransactionAmount == null || amount.compareTo(maxTransactionAmount) <= 0;
        return countOk && amountOk;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceLevel)) return false;
        ServiceLevel that = (ServiceLevel) o;
        return tier == that.tier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tier);
    }

    @Override
    public String toString() {
        return "ServiceLevel{" +
                "tier=" + tier +
                ", uptimeGuarantee=" + uptimeGuarantee +
                ", supportResponseTime=" + supportResponseTimeMinutes + "min" +
                '}';
    }
}