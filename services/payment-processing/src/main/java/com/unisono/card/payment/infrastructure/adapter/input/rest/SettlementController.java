package com.unisono.card.payment.infrastructure.adapter.input.rest;

import com.unisono.card.payment.application.dto.request.SettleTransactionCommand;
import com.unisono.card.payment.application.dto.response.ErrorResponse;
import com.unisono.card.payment.application.dto.response.SettlementResponse;
import com.unisono.card.payment.application.dto.response.TransactionResponse;
import com.unisono.card.payment.application.mapper.SettlementMapper;
import com.unisono.card.payment.application.mapper.TransactionMapper;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.port.input.SettleTransactionsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for settlement operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
@Tag(name = "Settlements", description = "APIs for settling transactions")
public class SettlementController {

    private final SettleTransactionsUseCase settleTransactionsUseCase;
    private final TransactionMapper transactionMapper;
    private final SettlementMapper settlementMapper;

    /**
     * Settle a single transaction
     */
    @PostMapping("/transaction/{transactionId}")
    @Operation(summary = "Settle single transaction", description = "Settles a single approved transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction settled successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Transaction cannot be settled",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TransactionResponse> settleTransaction(
            @PathVariable String transactionId) {

        log.info("Settling transaction: {}", transactionId);

        try {
            TransactionId txnId = TransactionId.of(transactionId);
            Transaction transaction = settleTransactionsUseCase.settleTransaction(txnId);
            TransactionResponse response = transactionMapper.toTransactionResponse(transaction);

            log.info("Transaction settled: {}, status: {}",
                    transaction.getTransactionId(), transaction.getStatus());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            log.error("Error settling transaction: {}", transactionId, ex);
            throw ex;
        }
    }

    /**
     * Settle multiple transactions in batch
     */
    @PostMapping("/batch")
    @Operation(summary = "Settle batch of transactions", description = "Settles multiple transactions in a single batch")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Batch settlement completed",
                    content = @Content(schema = @Schema(implementation = SettlementResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SettlementResponse> settleBatch(
            @Valid @RequestBody SettleTransactionCommand command) {

        log.info("Settling batch of {} transactions", command.getTransactionIds().size());

        try {
            List<TransactionId> transactionIds = command.getTransactionIds().stream()
                    .map(TransactionId::of)
                    .collect(Collectors.toList());

            SettleTransactionsUseCase.BatchSettlementResult result = settleTransactionsUseCase.settleBatch(transactionIds);

            String batchId = command.getSettlementBatchId() != null
                    ? command.getSettlementBatchId()
                    : "BATCH-" + UUID.randomUUID().toString().substring(0, 8);

            SettlementResponse response = settlementMapper.toSettlementResponse(
                    batchId,
                    result.settled(),
                    result.failed()
            );

            log.info("Batch settlement completed: {}, settled: {}/{}",
                    batchId, result.settled().size(), command.getTransactionIds().size());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            log.error("Error settling batch", ex);
            throw ex;
        }
    }

    /**
     * Settle all pending transactions
     */
    @PostMapping("/all-pending")
    @Operation(summary = "Settle all pending transactions", description = "Settles all approved transactions pending settlement")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pending transactions settled",
                    content = @Content(schema = @Schema(implementation = SettlementResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SettlementResponse> settleAllPending() {

        log.info("Settling all pending transactions");

        try {
            SettleTransactionsUseCase.BatchSettlementResult result = settleTransactionsUseCase.settleAllPending();

            String batchId = "AUTO-SETTLEMENT-" + UUID.randomUUID().toString().substring(0, 8);

            SettlementResponse response = settlementMapper.toSettlementResponse(
                    batchId,
                    result.settled(),
                    result.failed()
            );

            log.info("Auto-settlement completed: {}, settled: {} transactions",
                    batchId, result.settled().size());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            log.error("Error settling all pending transactions", ex);
            throw ex;
        }
    }
}
