package com.unisono.card.payment.infrastructure.adapter.input.rest;

import com.unisono.card.payment.application.dto.request.ProcessCashAdvanceCommand;
import com.unisono.card.payment.application.dto.request.ProcessPaymentCommand;
import com.unisono.card.payment.application.dto.response.ErrorResponse;
import com.unisono.card.payment.application.dto.response.PaymentResponse;
import com.unisono.card.payment.application.mapper.PaymentMapper;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.port.input.ProcessPaymentUseCase;
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
 * REST Controller for payment processing operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Processing", description = "APIs for processing payment transactions")
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final PaymentMapper paymentMapper;

    /**
     * Process a purchase transaction
     */
    @PostMapping("/purchase")
    @Operation(summary = "Process a purchase transaction", description = "Processes a card purchase transaction with authorization and fraud checks")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Payment processed successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "402", description = "Payment declined",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PaymentResponse> processPurchase(
            @Valid @RequestBody ProcessPaymentCommand command) {

        log.info("Processing purchase request for card: {}, amount: {} {}",
                command.getCardId(), command.getAmount(), command.getCurrency());

        try {
            Transaction transaction = processPaymentUseCase.processPurchase(command);
            PaymentResponse response = paymentMapper.toPaymentResponse(transaction);

            HttpStatus status = response.isApproved() ? HttpStatus.CREATED : HttpStatus.PAYMENT_REQUIRED;

            log.info("Purchase processed: {}, status: {}",
                    transaction.getTransactionId(), transaction.getStatus());

            return ResponseEntity.status(status).body(response);

        } catch (Exception ex) {
            log.error("Error processing purchase for card: {}", command.getCardId(), ex);
            throw ex;
        }
    }

    /**
     * Process a cash advance transaction
     */
    @PostMapping("/cash-advance")
    @Operation(summary = "Process a cash advance", description = "Processes an ATM cash advance transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cash advance processed successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "402", description = "Cash advance declined",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PaymentResponse> processCashAdvance(
            @Valid @RequestBody ProcessCashAdvanceCommand command) {

        log.info("Processing cash advance request for card: {}, amount: {} {}",
                command.getCardId(), command.getAmount(), command.getCurrency());

        try {
            Transaction transaction = processPaymentUseCase.processCashAdvance(command);
            PaymentResponse response = paymentMapper.toPaymentResponse(transaction);

            HttpStatus status = response.isApproved() ? HttpStatus.CREATED : HttpStatus.PAYMENT_REQUIRED;

            log.info("Cash advance processed: {}, status: {}",
                    transaction.getTransactionId(), transaction.getStatus());

            return ResponseEntity.status(status).body(response);

        } catch (Exception ex) {
            log.error("Error processing cash advance for card: {}", command.getCardId(), ex);
            throw ex;
        }
    }

    /**
     * Pre-authorize a purchase
     */
    @PostMapping("/pre-authorize")
    @Operation(summary = "Pre-authorize a purchase",
            description = "Authorizes a purchase and holds the credit without capturing it. "
                    + "The transaction stays AUTHORIZED until it is captured or reversed")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Purchase pre-authorized (AUTHORIZED)",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "402", description = "Pre-authorization declined",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PaymentResponse> preAuthorize(
            @Valid @RequestBody ProcessPaymentCommand command) {

        log.info("Processing pre-authorization request for card: {}, amount: {} {}",
                command.getCardId(), command.getAmount(), command.getCurrency());

        Transaction transaction = processPaymentUseCase.preAuthorize(command);
        PaymentResponse response = paymentMapper.toPaymentResponse(transaction);

        HttpStatus status = response.isApproved() ? HttpStatus.CREATED : HttpStatus.PAYMENT_REQUIRED;

        log.info("Pre-authorization processed: {}, status: {}",
                transaction.getTransactionId(), transaction.getStatus());

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Capture a pre-authorized transaction
     */
    @PostMapping("/{transactionId}/capture")
    @Operation(summary = "Capture pre-authorized transaction", description = "Captures funds for a previously authorized transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction captured successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Transaction cannot be captured",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PaymentResponse> captureTransaction(
            @PathVariable String transactionId) {

        log.info("Capturing transaction: {}", transactionId);

        try {
            TransactionId txnId = TransactionId.of(transactionId);
            Transaction transaction = processPaymentUseCase.captureTransaction(txnId);
            PaymentResponse response = paymentMapper.toPaymentResponse(transaction);

            log.info("Transaction captured: {}, status: {}",
                    transaction.getTransactionId(), transaction.getStatus());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            log.error("Error capturing transaction: {}", transactionId, ex);
            throw ex;
        }
    }
}
