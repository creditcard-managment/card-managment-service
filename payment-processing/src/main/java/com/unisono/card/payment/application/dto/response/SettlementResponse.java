package com.unisono.card.payment.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for settlement processing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementResponse {

    private String settlementBatchId;

    private Integer totalTransactions;
    private Integer settledCount;
    private Integer failedCount;

    private BigDecimal totalAmount;
    private String currency;

    private List<String> settledTransactionIds;
    private List<String> failedTransactionIds;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime settlementDate;

    private String status;
    private String message;
}
