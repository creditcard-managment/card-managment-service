package com.unisono.card.payment.infrastructure.adapter.input.rest;

import com.unisono.card.payment.application.dto.response.ErrorResponse;
import com.unisono.card.payment.application.mapper.ErrorMapper;
import com.unisono.card.payment.domain.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Global exception handler for payment processing REST controllers
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorMapper errorMapper;

    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {

        log.warn("Validation error: {}", ex.getMessage());

        ErrorResponse error = errorMapper.toErrorResponse(ex, request.getDescription(false));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle transaction not found
     */
    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFound(
            TransactionNotFoundException ex, WebRequest request) {

        log.warn("Transaction not found: {}", ex.getMessage());

        ErrorResponse error = errorMapper.toErrorResponse(
                "TRANSACTION_NOT_FOUND",
                ex.getMessage(),
                "The requested transaction does not exist",
                request.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle invalid transaction operation
     */
    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransaction(
            InvalidTransactionException ex, WebRequest request) {

        log.warn("Invalid transaction operation: {}", ex.getMessage());

        ErrorResponse error = errorMapper.toErrorResponse(
                "INVALID_TRANSACTION",
                ex.getMessage(),
                "The transaction operation is not valid",
                request.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle transaction already processed
     */
    @ExceptionHandler(TransactionAlreadyProcessedException.class)
    public ResponseEntity<ErrorResponse> handleTransactionAlreadyProcessed(
            TransactionAlreadyProcessedException ex, WebRequest request) {

        log.warn("Transaction already processed: {}", ex.getMessage());

        ErrorResponse error = errorMapper.toErrorResponse(
                "TRANSACTION_ALREADY_PROCESSED",
                ex.getMessage(),
                "The transaction is already in a final state",
                request.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle insufficient funds
     */
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(
            InsufficientFundsException ex, WebRequest request) {

        log.warn("Insufficient funds: {}", ex.getMessage());

        ErrorResponse error = errorMapper.toErrorResponse(
                "INSUFFICIENT_FUNDS",
                ex.getMessage(),
                String.format("Requested: %s, Available: %s, Shortfall: %s",
                        ex.getRequestedAmount(), ex.getAvailableAmount(), ex.getShortfall()),
                request.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(error);
    }

    /**
     * Handle card validation errors
     */
    @ExceptionHandler(CardValidationException.class)
    public ResponseEntity<ErrorResponse> handleCardValidation(
            CardValidationException ex, WebRequest request) {

        log.warn("Card validation failed: {}", ex.getMessage());

        ErrorResponse error = errorMapper.toErrorResponse(
                "CARD_VALIDATION_FAILED",
                ex.getMessage(),
                "Card validation failed",
                request.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle authorization failures
     */
    @ExceptionHandler(AuthorizationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationFailed(
            AuthorizationFailedException ex, WebRequest request) {

        log.warn("Authorization failed: {}", ex.getMessage());

        ErrorResponse error = errorMapper.toErrorResponse(
                "AUTHORIZATION_FAILED",
                ex.getMessage(),
                String.format("Response code: %s, Reason: %s", ex.getResponseCode(), ex.getReason()),
                request.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(error);
    }

    /**
     * Handle fraud detection
     */
    @ExceptionHandler(FraudDetectedException.class)
    public ResponseEntity<ErrorResponse> handleFraudDetected(
            FraudDetectedException ex, WebRequest request) {

        log.error("Fraud detected: {}", ex.getMessage());

        ErrorResponse error = errorMapper.toErrorResponse(
                "FRAUD_DETECTED",
                "Transaction blocked due to suspected fraud",
                String.format("Risk score: %d, Reason: %s", ex.getRiskScore(), ex.getFraudReason()),
                request.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle merchant validation errors
     */
    @ExceptionHandler(MerchantValidationException.class)
    public ResponseEntity<ErrorResponse> handleMerchantValidation(
            MerchantValidationException ex, WebRequest request) {

        log.warn("Merchant validation failed: {}", ex.getMessage());

        ErrorResponse error = errorMapper.toErrorResponse(
                "MERCHANT_VALIDATION_FAILED",
                ex.getMessage(),
                "Merchant validation failed",
                request.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle generic payment exceptions
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(
            PaymentException ex, WebRequest request) {

        log.error("Payment processing error: {}", ex.getMessage(), ex);

        ErrorResponse error = errorMapper.toErrorResponse(
                "PAYMENT_ERROR",
                ex.getMessage(),
                "An error occurred during payment processing",
                request.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {

        log.error("Unexpected error: {}", ex.getMessage(), ex);

        ErrorResponse error = errorMapper.toErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                ex.getClass().getSimpleName(),
                request.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
