package com.unisono.card.payment.infrastructure.adapter.input.rest;

import com.unisono.card.payment.application.dto.response.ErrorResponse;
import com.unisono.card.payment.application.dto.response.TransactionResponse;
import com.unisono.card.payment.application.dto.response.TransactionSummaryResponse;
import com.unisono.card.payment.application.mapper.TransactionMapper;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.model.TransactionStatus;
import com.unisono.card.payment.domain.port.input.QueryTransactionUseCase;
import com.unisono.card.shared.domain.valueobject.CardId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST Controller for querying transaction information
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Queries", description = "APIs for querying transaction information")
public class TransactionController {

    private final QueryTransactionUseCase queryTransactionUseCase;
    private final TransactionMapper transactionMapper;

    /**
     * Get transaction by ID
     */
    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction by ID", description = "Retrieves detailed information about a specific transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction found",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TransactionResponse> getTransactionById(
            @PathVariable String transactionId) {

        log.debug("Getting transaction: {}", transactionId);

        TransactionId txnId = TransactionId.of(transactionId);
        Transaction transaction = queryTransactionUseCase.findById(txnId)
                .orElseThrow(() -> new com.unisono.card.payment.domain.exception.TransactionNotFoundException(txnId));

        TransactionResponse response = transactionMapper.toTransactionResponse(transaction);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all transactions for a card
     */
    @GetMapping("/card/{cardId}")
    @Operation(summary = "Get transactions by card", description = "Retrieves all transactions for a specific card")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions found",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
    })
    public ResponseEntity<List<TransactionResponse>> getTransactionsByCard(
            @PathVariable String cardId) {

        log.debug("Getting transactions for card: {}", cardId);

        CardId card = CardId.of(cardId);
        List<Transaction> transactions = queryTransactionUseCase.findByCardId(card);
        List<TransactionResponse> response = transactionMapper.toTransactionResponseList(transactions);

        return ResponseEntity.ok(response);
    }

    /**
     * Get transactions by card and status
     */
    @GetMapping("/card/{cardId}/status/{status}")
    @Operation(summary = "Get transactions by card and status", description = "Retrieves transactions for a card filtered by status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions found",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
    })
    public ResponseEntity<List<TransactionResponse>> getTransactionsByCardAndStatus(
            @PathVariable String cardId,
            @PathVariable TransactionStatus status) {

        log.debug("Getting transactions for card: {} with status: {}", cardId, status);

        CardId card = CardId.of(cardId);
        List<Transaction> transactions = queryTransactionUseCase.findByCardIdAndStatus(card, status);
        List<TransactionResponse> response = transactionMapper.toTransactionResponseList(transactions);

        return ResponseEntity.ok(response);
    }

    /**
     * Get transactions by card in date range
     */
    @GetMapping("/card/{cardId}/range")
    @Operation(summary = "Get transactions by date range", description = "Retrieves transactions for a card within a specific date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions found",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
    })
    public ResponseEntity<List<TransactionResponse>> getTransactionsByDateRange(
            @PathVariable String cardId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        log.debug("Getting transactions for card: {} between {} and {}", cardId, fromDate, toDate);

        CardId card = CardId.of(cardId);
        List<Transaction> transactions = queryTransactionUseCase.findByCardIdAndDateRange(card, fromDate, toDate);
        List<TransactionResponse> response = transactionMapper.toTransactionResponseList(transactions);

        return ResponseEntity.ok(response);
    }

    /**
     * Get transaction summary for a card
     */
    @GetMapping("/card/{cardId}/summary")
    @Operation(summary = "Get transaction summary", description = "Retrieves transaction statistics and summary for a card")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Summary generated",
                    content = @Content(schema = @Schema(implementation = TransactionSummaryResponse.class)))
    })
    public ResponseEntity<TransactionSummaryResponse> getTransactionSummary(
            @PathVariable String cardId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        log.debug("Getting transaction summary for card: {}", cardId);

        CardId card = CardId.of(cardId);
        List<Transaction> transactions;

        if (fromDate != null && toDate != null) {
            transactions = queryTransactionUseCase.findByCardIdAndDateRange(card, fromDate, toDate);
        } else {
            transactions = queryTransactionUseCase.findByCardId(card);
        }

        TransactionSummaryResponse summary = transactionMapper.toTransactionSummary(transactions);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get pending settlement transactions
     */
    @GetMapping("/pending-settlement")
    @Operation(summary = "Get pending settlement transactions", description = "Retrieves all transactions pending settlement")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions found",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
    })
    public ResponseEntity<List<TransactionResponse>> getPendingSettlement() {

        log.debug("Getting pending settlement transactions");

        List<Transaction> transactions = queryTransactionUseCase.findPendingSettlement();
        List<TransactionResponse> response = transactionMapper.toTransactionResponseList(transactions);

        return ResponseEntity.ok(response);
    }
}
