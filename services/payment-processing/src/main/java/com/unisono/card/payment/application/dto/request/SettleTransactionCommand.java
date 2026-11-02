package com.unisono.card.payment.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Command for settling transactions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettleTransactionCommand {

    @NotEmpty(message = "Transaction IDs cannot be empty")
    private List<String> transactionIds;

    private String settlementBatchId;

    private String initiatedBy;
}
