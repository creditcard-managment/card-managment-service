package com.unisono.card.payment.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Summary response for multiple transactions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSummaryResponse {

    private Long totalTransactions;
    private Long approvedCount;
    private Long declinedCount;
    private Long reversedCount;
    private Long pendingCount;

    private BigDecimal totalAmount;
    private String currency;

    private BigDecimal approvedAmount;
    private BigDecimal declinedAmount;

    private Double averageTransactionAmount;
    private BigDecimal highestTransactionAmount;
    private BigDecimal lowestTransactionAmount;
}
