package com.unisono.card.payment.infrastructure.adapter.input.rest;

import com.unisono.card.payment.application.dto.request.ReverseTransactionCommand;
import com.unisono.card.payment.application.dto.response.ErrorResponse;
import com.unisono.card.payment.application.dto.response.TransactionResponse;
import com.unisono.card.payment.application.mapper.TransactionMapper;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.port.input.ReverseTransactionUseCase;
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

/**
 * REST Controller for transaction reversal operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/reversals")
@RequiredArgsConstructor
@Tag(name = "Reversals", description = "APIs for reversing transactions")
public class ReversalController {

    private final ReverseTransactionUseCase reverseTransactionUseCase;
    private final TransactionMapper transactionMapper;

    /**
     * Reverse a transaction
     */
    @PostMapping
    @Operation(summary = "Reverse transaction", description = "Reverses a transaction and releases reserved funds")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction reversed successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Transaction cannot be reversed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TransactionResponse> reverseTransaction(
            @Valid @RequestBody ReverseTransactionCommand command) {

        log.info("Reversing transaction: {}, reason: {}",
                command.getTransactionId(), command.getReason());

        try {
            TransactionId txnId = TransactionId.of(command.getTransactionId());
            Transaction transaction = reverseTransactionUseCase.reverseTransaction(txnId, command.getReason());
            TransactionResponse response = transactionMapper.toTransactionResponse(transaction);

            log.info("Transaction reversed: {}, status: {}",
                    transaction.getTransactionId(), transaction.getStatus());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            log.error("Error reversing transaction: {}", command.getTransactionId(), ex);
            throw ex;
        }
    }

    /**
     * Auto-reverse a failed transaction
     */
    @PostMapping("/{transactionId}/auto")
    @Operation(summary = "Auto-reverse transaction", description = "Automatically reverses a failed or timed-out transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction auto-reversed successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TransactionResponse> autoReverseTransaction(
            @PathVariable String transactionId) {

        log.info("Auto-reversing transaction: {}", transactionId);

        try {
            TransactionId txnId = TransactionId.of(transactionId);
            Transaction transaction = reverseTransactionUseCase.autoReverse(txnId);
            TransactionResponse response = transactionMapper.toTransactionResponse(transaction);

            log.info("Transaction auto-reversed: {}, status: {}",
                    transaction.getTransactionId(), transaction.getStatus());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            log.error("Error auto-reversing transaction: {}", transactionId, ex);
            throw ex;
        }
    }
}
