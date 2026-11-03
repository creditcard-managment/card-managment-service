package com.unisono.card.data.infrastructure.adapter.input.rest;

import com.unisono.card.data.application.dto.request.ValidateCVVRequest;
import com.unisono.card.data.domain.port.input.ValidateCVVUseCase;
import com.unisono.card.shared.domain.valueobject.CardId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller para validación de CVV (pagos con tarjeta no presente)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Tag(name = "CVV Validation", description = "Validación del CVV de una tarjeta")
public class CVVController {

    private final ValidateCVVUseCase validateCVVUseCase;

    @PostMapping("/{cardId}/validate-cvv")
    @Operation(summary = "Validar CVV", description = "Valida si el CVV proporcionado coincide con el de la tarjeta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validación realizada: { \"valid\": true | false }"),
            @ApiResponse(responseCode = "400", description = "CVV con formato inválido"),
            @ApiResponse(responseCode = "404", description = "Tarjeta no encontrada"),
            @ApiResponse(responseCode = "422", description = "Tarjeta bloqueada")
    })
    public ResponseEntity<Map<String, Boolean>> validateCVV(
            @Parameter(description = "ID de la tarjeta")
            @PathVariable String cardId,

            @Valid @RequestBody ValidateCVVRequest request
    ) {
        log.debug("Validating CVV for card: {}", cardId);

        boolean isValid = validateCVVUseCase.execute(
                new ValidateCVVUseCase.ValidateCVVCommand(CardId.of(cardId), request.getCvv()));

        return ResponseEntity.ok(Map.of("valid", isValid));
    }
}
