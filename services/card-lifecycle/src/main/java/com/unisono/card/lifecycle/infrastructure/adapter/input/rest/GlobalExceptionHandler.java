package com.unisono.card.lifecycle.infrastructure.adapter.input.rest;

import com.unisono.card.lifecycle.domain.exception.CardDataIntegrationException;
import com.unisono.card.lifecycle.domain.exception.InvalidStateTransitionException;
import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Traduce las excepciones de dominio a respuestas HTTP con el mismo formato que card-data.
 * Lo que no se maneja acá sigue saliendo con el error por defecto de Spring Boot.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        return build(HttpStatus.BAD_REQUEST, "Validation Error", ex);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidStateTransitionException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid State Transition", ex);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Business Rule Violation", ex);
    }

    @ExceptionHandler(CardDataIntegrationException.class)
    public ResponseEntity<ErrorResponse> handleCardDataIntegration(CardDataIntegrationException ex) {
        return build(HttpStatus.BAD_GATEWAY, "Bad Gateway", ex);
    }

    private static ResponseEntity<ErrorResponse> build(HttpStatus status, String error, Exception ex) {
        log.warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
        return ResponseEntity.status(status).body(new ErrorResponse(
                LocalDateTime.now(), status.value(), error, ex.getMessage()));
    }

    public record ErrorResponse(LocalDateTime timestamp, Integer status, String error, String message) {}
}
