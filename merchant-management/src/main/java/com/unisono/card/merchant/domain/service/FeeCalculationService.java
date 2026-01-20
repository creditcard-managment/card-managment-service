package com.unisono.card.merchant.domain.service;

import com.unisono.card.merchant.domain.model.fees.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Servicio de dominio para cálculo de comisiones
 */
@Slf4j
@Service
public class FeeCalculationService {

    /**
     * Calcula el fee total para una transacción
     */
    public BigDecimal calculateTotalTransactionFee(
            MerchantFeeStructure feeStructure,
            BigDecimal transactionAmount,
            String cardBrand,
            String transactionType
    ) {
        Objects.requireNonNull(feeStructure, "Fee structure cannot be null");
        Objects.requireNonNull(transactionAmount, "Transaction amount cannot be null");
        Objects.requireNonNull(cardBrand, "Card brand cannot be null");
        Objects.requireNonNull(transactionType, "Transaction type cannot be null");

        if (!feeStructure.isCurrentlyActive()) {
            throw new IllegalStateException("Fee structure is not currently active");
        }

        BigDecimal totalFee = feeStructure.calculateTotalFee(
                transactionAmount,
                cardBrand,
                transactionType
        );

        log.debug("Calculated fee: {} for amount: {}, brand: {}, type: {}",
                totalFee, transactionAmount, cardBrand, transactionType);

        return totalFee;
    }

    /**
     * Calcula el monto neto que recibirá el merchant después de fees
     */
    public BigDecimal calculateNetAmount(
            BigDecimal transactionAmount,
            BigDecimal totalFee
    ) {
        Objects.requireNonNull(transactionAmount, "Transaction amount cannot be null");
        Objects.requireNonNull(totalFee, "Total fee cannot be null");

        BigDecimal netAmount = transactionAmount.subtract(totalFee);

        if (netAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Net amount cannot be negative");
        }

        return netAmount;
    }

    /**
     * Calcula el fee efectivo como porcentaje
     */
    public BigDecimal calculateEffectiveFeePercentage(
            BigDecimal transactionAmount,
            BigDecimal totalFee
    ) {
        Objects.requireNonNull(transactionAmount, "Transaction amount cannot be null");
        Objects.requireNonNull(totalFee, "Total fee cannot be null");

        if (transactionAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return totalFee
                .divide(transactionAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * Calcula el fee de chargeback
     */
    public BigDecimal calculateChargebackFee(
            MerchantFeeStructure feeStructure,
            Integer chargebackCount
    ) {
        Objects.requireNonNull(feeStructure, "Fee structure cannot be null");
        Objects.requireNonNull(chargebackCount, "Chargeback count cannot be null");

        return feeStructure.getChargebackFee()
                .multiply(new BigDecimal(chargebackCount));
    }
}
