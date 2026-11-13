package com.unisono.card.data.infrastructure.adapter.input.rest;

import com.unisono.card.data.application.dto.request.ResetPINRequest;
import com.unisono.card.data.application.dto.request.ValidatePINRequest;
import com.unisono.card.data.application.dto.response.CardResponse;
import com.unisono.card.data.application.mapper.CardDtoMapper;
import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.port.input.ResetPINUseCase;
import com.unisono.card.data.domain.port.input.ValidatePINUseCase;
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
 * REST Controller para gestión de PIN
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Tag(name = "PIN Management", description = "Endpoints para validación y reset de PIN")
public class PINController {

    private final ValidatePINUseCase validatePINUseCase;
    private final ResetPINUseCase resetPINUseCase;
    private final CardDtoMapper mapper;

    /**
     * Validar PIN
     */
    @PostMapping("/{cardId}/validate-pin")
    @Operation(summary = "Validar PIN", description = "Valida el PIN, cifrado en un PIN block ISO 9564 formato 4 "
            + "(pinBlock + pinKeyId). Un PIN incorrecto suma un intento fallido; un bloque que no se puede "
            + "descifrar responde 400 sin sumarlo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validación exitosa"),
            @ApiResponse(responseCode = "400", description = "Request inválido, PIN block indescifrable o clave de PIN desconocida"),
            @ApiResponse(responseCode = "404", description = "Tarjeta no encontrada")
    })
    public ResponseEntity<Map<String, Boolean>> validatePIN(
            @Parameter(description = "ID de la tarjeta")
            @PathVariable String cardId,

            @Valid @RequestBody ValidatePINRequest request
    ) {
        log.debug("Validating PIN for card: {}", cardId);

        ValidatePINUseCase.ValidatePINCommand command = request.getPinBlock() != null && !request.getPinBlock().isBlank()
                ? ValidatePINUseCase.ValidatePINCommand.ofPinBlock(CardId.of(cardId), request.getPinBlock(), request.getPinKeyId())
                : new ValidatePINUseCase.ValidatePINCommand(CardId.of(cardId), request.getPin());

        boolean isValid = validatePINUseCase.execute(command);

        if (isValid) {
            log.debug("PIN validated successfully for card: {}", cardId);
        } else {
            log.warn("Invalid PIN attempt for card: {}", cardId);
        }

        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    /**
     * Resetear PIN
     */
    @PostMapping("/{cardId}/reset-pin")
    @Operation(summary = "Resetear PIN", description = "Cambia el PIN de una tarjeta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PIN actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "PIN actual incorrecto o nuevo PIN inválido"),
            @ApiResponse(responseCode = "404", description = "Tarjeta no encontrada")
    })
    public ResponseEntity<CardResponse> resetPIN(
            @Parameter(description = "ID de la tarjeta")
            @PathVariable String cardId,

            @Valid @RequestBody ResetPINRequest request
    ) {
        log.info("Resetting PIN for card: {}", cardId);

        ResetPINUseCase.ResetPINCommand command = new ResetPINUseCase.ResetPINCommand(
                CardId.of(cardId),
                request.getNewPin(),
                request.getOldPin()
        );

        Card card = resetPINUseCase.execute(command);

        CardResponse response = mapper.toResponse(card);

        log.info("PIN reset successfully for card: {}", cardId);

        return ResponseEntity.ok(response);
    }
}
