package com.unisono.card.payment.application.dto.request;

import com.unisono.card.payment.domain.model.TransactionStatus;
import com.unisono.card.payment.domain.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request for querying transactions with filters
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryTransactionRequest {

    private String cardId;
    private String merchantId;
    private TransactionStatus status;
    private TransactionType type;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;

    // Pagination
    private Integer page = 0;
    private Integer size = 20;

    // Sorting
    private String sortBy = "transactionDate";
    private String sortDirection = "DESC";
}
