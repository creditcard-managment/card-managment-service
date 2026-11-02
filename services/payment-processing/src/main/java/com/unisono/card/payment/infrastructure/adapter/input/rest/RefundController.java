package com.unisono.card.payment.infrastructure.adapter.input.rest;

import com.unisono.card.payment.application.dto.request.ProcessRefundCommand;
import com.unisono.card.payment.application.dto.response.ErrorResponse;
import com.unisono.card.payment.application.dto.response.RefundResponse;
import com.unisono.card.payment.application.mapper.RefundMapper;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.port.input.ProcessRefundUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for refund operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/refunds")
@RequiredArgsConstructor
@Tag(name = "Refunds", description = "APIs for processing refund transactions")
public class RefundController {

    private final ProcessRefundUseCase processRefundUseCase;
    private final RefundMapper refundMapper;

    /**
     * Process a full refund
     */
    @PostMapping("/full")
    @Operation(summary = "Process full refund", description = "Processes a complete refund for an original transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Refund processed successfully",
                    content = @Content(schema = @Schema(implementation = RefundResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or transaction cannot be refunded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Original transaction not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RefundResponse> processFullRefund(
            @Valid @RequestBody ProcessRefundCommand command) {

        log.info("Processing full refund request for transaction: {}, amount: {} {}",
                command.getOriginalTransactionId(), command.getRefundAmount(), command.getCurrency());

        try {
            Transaction refundTransaction = processRefundUseCase.processFullRefund(command);
            RefundResponse response = refundMapper.toRefundResponse(refundTransaction);

            log.info("Full refund processed: {}, status: {}",
                    refundTransaction.getTransactionId(), refundTransaction.getStatus());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception ex) {
            log.error("Error processing full refund for transaction: {}",
                    command.getOriginalTransactionId(), ex);
            throw ex;
        }
    }

    /**
     * Process a partial refund
     */
    @PostMapping("/partial")
    @Operation(summary = "Process partial refund", description = "Processes a partial refund for an original transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Partial refund processed successfully",
                    content = @Content(schema = @Schema(implementation = RefundResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or refund amount exceeds original",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Original transaction not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RefundResponse> processPartialRefund(
            @Valid @RequestBody ProcessRefundCommand command) {

        log.info("Processing partial refund request for transaction: {}, amount: {} {}",
                command.getOriginalTransactionId(), command.getRefundAmount(), command.getCurrency());

        try {
            Transaction refundTransaction = processRefundUseCase.processPartialRefund(command);
            RefundResponse response = refundMapper.toRefundResponse(refundTransaction);

            log.info("Partial refund processed: {}, status: {}",
                    refundTransaction.getTransactionId(), refundTransaction.getStatus());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception ex) {
            log.error("Error processing partial refund for transaction: {}",
                    command.getOriginalTransactionId(), ex);
            throw ex;
        }
    }
}
