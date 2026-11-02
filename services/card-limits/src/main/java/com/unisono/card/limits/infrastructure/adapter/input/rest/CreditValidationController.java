package com.unisono.card.limits.infrastructure.adapter.input.rest;

import com.unisono.card.limits.application.dto.request.ValidatePurchaseRequest;
import com.unisono.card.limits.application.dto.response.ValidationResultResponse;
import com.unisono.card.limits.application.mapper.CreditLimitDtoMapper;
import com.unisono.card.limits.domain.port.input.ValidatePurchaseUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller para validación de transacciones
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/credit-limits/validate")
@RequiredArgsConstructor
@Tag(name = "Credit Validation", description = "Endpoints para validación de transacciones")
public class CreditValidationController {

    private final ValidatePurchaseUseCase validatePurchaseUseCase;
    private final CreditLimitDtoMapper mapper;

    @PostMapping("/purchase")
    @Operation(summary = "Validar compra",
            description = "Valida si una compra puede ser realizada considerando todos los límites")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validación realizada"),
            @ApiResponse(responseCode = "404", description = "Tarjeta o límite no encontrado")
    })
    public ResponseEntity<ValidationResultResponse> validatePurchase(
            @Valid @RequestBody ValidatePurchaseRequest request
    ) {
        log.info("Validating purchase for card: {} - Amount: {}",
                request.getCardId(), request.getAmount());

        ValidatePurchaseUseCase.ValidatePurchaseCommand command =
                mapper.toValidatePurchaseCommand(request);

        ValidatePurchaseUseCase.ValidationResult result =
                validatePurchaseUseCase.validatePurchase(command);

        ValidationResultResponse response = mapper.toValidationResponse(result, request.getCurrency());

        if (result.approved()) {
            log.info("Purchase validated - APPROVED");
        } else {
            log.warn("Purchase validated - REJECTED: {}", result.reason());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/cash-advance")
    @Operation(summary = "Validar adelanto en efectivo",
            description = "Valida si un adelanto en efectivo puede ser realizado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validación realizada"),
            @ApiResponse(responseCode = "404", description = "Tarjeta o límite no encontrado")
    })
    public ResponseEntity<ValidationResultResponse> validateCashAdvance(
            @Valid @RequestBody ValidatePurchaseRequest request
    ) {
        log.info("Validating cash advance for card: {} - Amount: {}",
                request.getCardId(), request.getAmount());

        ValidatePurchaseUseCase.ValidateCashAdvanceCommand command =
                mapper.toValidateCashAdvanceCommand(request);

        ValidatePurchaseUseCase.ValidationResult result =
                validatePurchaseUseCase.validateCashAdvance(command);

        ValidationResultResponse response = mapper.toValidationResponse(result, request.getCurrency());

        if (result.approved()) {
            log.info("Cash advance validated - APPROVED");
        } else {
            log.warn("Cash advance validated - REJECTED: {}", result.reason());
        }

        return ResponseEntity.ok(response);
    }
}
