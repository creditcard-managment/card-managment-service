package com.unisono.card.payment.application.mapper;

import com.unisono.card.payment.application.dto.response.ErrorResponse;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting exceptions to error responses
 */
@Component
public class ErrorMapper {

    /**
     * Map validation exception to error response
     */
    public ErrorResponse toErrorResponse(MethodArgumentNotValidException ex, String path) {
        List<ErrorResponse.ValidationError> validationErrors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    String fieldName = error instanceof FieldError
                            ? ((FieldError) error).getField()
                            : error.getObjectName();

                    Object rejectedValue = error instanceof FieldError
                            ? ((FieldError) error).getRejectedValue()
                            : null;

                    return ErrorResponse.ValidationError.builder()
                            .field(fieldName)
                            .message(error.getDefaultMessage())
                            .rejectedValue(rejectedValue)
                            .build();
                })
                .collect(Collectors.toList());

        return ErrorResponse.builder()
                .errorCode("VALIDATION_ERROR")
                .message("Validation failed for one or more fields")
                .details("Please check the validationErrors field for details")
                .timestamp(LocalDateTime.now())
                .path(path)
                .validationErrors(validationErrors)
                .build();
    }

    /**
     * Map generic exception to error response
     */
    public ErrorResponse toErrorResponse(Exception ex, String errorCode, String path) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(ex.getMessage())
                .details(ex.getClass().getSimpleName())
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }

    /**
     * Map domain exception to error response with custom code
     */
    public ErrorResponse toErrorResponse(String errorCode, String message, String details, String path) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
}
