package com.unisono.card.payment.application.mapper;

import com.unisono.card.payment.application.dto.response.SettlementResponse;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.model.TransactionStatus;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for settlement operations
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface SettlementMapper {

    /**
     * Create settlement response from the batch result
     */
    default SettlementResponse toSettlementResponse(
            String settlementBatchId,
            List<Transaction> transactions,
            List<TransactionId> failed) {

        List<Transaction> settled = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.SETTLED)
                .collect(Collectors.toList());

        int total = transactions.size() + failed.size();

        BigDecimal totalAmount = settled.stream()
                .map(t -> t.getAmount().getEffectiveAmount().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String currency = transactions.isEmpty()
                ? "USD"
                : transactions.get(0).getAmount().getEffectiveAmount().getCurrency().toString();

        List<String> settledIds = settled.stream()
                .map(t -> t.getTransactionId().toString())
                .collect(Collectors.toList());

        List<String> failedIds = failed.stream()
                .map(TransactionId::toString)
                .collect(Collectors.toList());

        String status = failed.isEmpty() ? "COMPLETED" : "PARTIAL";
        String message = String.format("Settlement completed: %d/%d transactions settled successfully",
                settled.size(), total);

        return SettlementResponse.builder()
                .settlementBatchId(settlementBatchId)
                .totalTransactions(total)
                .settledCount(settled.size())
                .failedCount(failed.size())
                .totalAmount(totalAmount)
                .currency(currency)
                .settledTransactionIds(settledIds)
                .failedTransactionIds(failedIds)
                .settlementDate(LocalDateTime.now())
                .status(status)
                .message(message)
                .build();
    }
}
