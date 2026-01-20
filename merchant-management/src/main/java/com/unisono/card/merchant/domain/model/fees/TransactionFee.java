package com.unisono.card.merchant.domain.model.fees;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Set;

/**
 * Comisión por transacción
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TransactionFee {

    private FeeType feeType;
    private BigDecimal percentageFee; // Porcentaje (ej: 2.5%)
    private BigDecimal fixedFee; // Monto fijo (ej: $0.30)
    private Set<String> applicableCardBrands; // VISA, MASTERCARD, etc.
    private Set<String> applicableTransactionTypes; // PURCHASE, REFUND, etc.
    private BigDecimal minimumFee;
    private BigDecimal maximumFee;

    public static TransactionFee percentage(
            BigDecimal percentage,
            Set<String> cardBrands,
            Set<String> transactionTypes
    ) {
        return new TransactionFee(
                FeeType.PERCENTAGE,
                percentage,
                BigDecimal.ZERO,
                cardBrands,
                transactionTypes,
                BigDecimal.ZERO,
                null
        );
    }

    public static TransactionFee fixed(
            BigDecimal fixedAmount,
            Set<String> cardBrands,
            Set<String> transactionTypes
    ) {
        return new TransactionFee(
                FeeType.FIXED,
                BigDecimal.ZERO,
                fixedAmount,
                cardBrands,
                transactionTypes,
                BigDecimal.ZERO,
                null
        );
    }

    public static TransactionFee combined(
            BigDecimal percentage,
            BigDecimal fixedAmount,
            Set<String> cardBrands,
            Set<String> transactionTypes
    ) {
        return new TransactionFee(
                FeeType.PERCENTAGE_PLUS_FIXED,
                percentage,
                fixedAmount,
                cardBrands,
                transactionTypes,
                BigDecimal.ZERO,
                null
        );
    }

    public static TransactionFee of(
            FeeType feeType,
            BigDecimal percentageFee,
            BigDecimal fixedFee,
            Set<String> applicableCardBrands,
            Set<String> applicableTransactionTypes,
            BigDecimal minimumFee,
            BigDecimal maximumFee
    ) {
        Objects.requireNonNull(feeType, "Fee type cannot be null");
        Objects.requireNonNull(applicableCardBrands, "Applicable card brands cannot be null");
        Objects.requireNonNull(applicableTransactionTypes, "Applicable transaction types cannot be null");

        return new TransactionFee(
                feeType,
                percentageFee != null ? percentageFee : BigDecimal.ZERO,
                fixedFee != null ? fixedFee : BigDecimal.ZERO,
                applicableCardBrands,
                applicableTransactionTypes,
                minimumFee != null ? minimumFee : BigDecimal.ZERO,
                maximumFee
        );
    }

    public BigDecimal calculateFee(BigDecimal transactionAmount) {
        Objects.requireNonNull(transactionAmount, "Transaction amount cannot be null");

        BigDecimal calculatedFee = BigDecimal.ZERO;

        switch (feeType) {
            case PERCENTAGE:
                calculatedFee = transactionAmount
                        .multiply(percentageFee)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                break;

            case FIXED:
                calculatedFee = fixedFee;
                break;

            case PERCENTAGE_PLUS_FIXED:
                BigDecimal percentagePart = transactionAmount
                        .multiply(percentageFee)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                calculatedFee = percentagePart.add(fixedFee);
                break;

            case TIERED:
                // Implementación simplificada, podría ser más compleja
                calculatedFee = calculateTieredFee(transactionAmount);
                break;
        }

        // Aplicar mínimo y máximo
        if (calculatedFee.compareTo(minimumFee) < 0) {
            calculatedFee = minimumFee;
        }

        if (maximumFee != null && calculatedFee.compareTo(maximumFee) > 0) {
            calculatedFee = maximumFee;
        }

        return calculatedFee.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTieredFee(BigDecimal amount) {
        // Implementación simplificada de fee escalonado
        // En producción, esto tendría niveles configurables
        if (amount.compareTo(new BigDecimal("1000")) <= 0) {
            return amount.multiply(new BigDecimal("0.025")); // 2.5%
        } else if (amount.compareTo(new BigDecimal("10000")) <= 0) {
            return amount.multiply(new BigDecimal("0.020")); // 2.0%
        } else {
            return amount.multiply(new BigDecimal("0.015")); // 1.5%
        }
    }

    public boolean isApplicable(String cardBrand, String transactionType) {
        boolean brandMatches = applicableCardBrands.isEmpty() ||
                applicableCardBrands.contains(cardBrand.toUpperCase());
        boolean typeMatches = applicableTransactionTypes.isEmpty() ||
                applicableTransactionTypes.contains(transactionType.toUpperCase());
        return brandMatches && typeMatches;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionFee)) return false;
        TransactionFee that = (TransactionFee) o;
        return feeType == that.feeType &&
                Objects.equals(percentageFee, that.percentageFee) &&
                Objects.equals(fixedFee, that.fixedFee);
    }

    @Override
    public int hashCode() {
        return Objects.hash(feeType, percentageFee, fixedFee);
    }

    @Override
    public String toString() {
        return "TransactionFee{" +
                "type=" + feeType +
                ", percentage=" + percentageFee +
                ", fixed=" + fixedFee +
                '}';
    }
}
