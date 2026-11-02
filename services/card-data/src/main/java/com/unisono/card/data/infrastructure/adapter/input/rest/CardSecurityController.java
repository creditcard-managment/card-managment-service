package com.unisono.card.data.infrastructure.adapter.input.rest;

import com.unisono.card.data.application.dto.request.BlockCardRequest;
import com.unisono.card.data.application.dto.response.CardResponse;
import com.unisono.card.data.application.mapper.CardDtoMapper;
import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.port.input.BlockCardUseCase;
import com.unisono.card.data.domain.port.input.UnblockCardUseCase;
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

/**
 * REST Controller para seguridad de tarjetas
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Tag(name = "Card Security", description = "Endpoints para bloqueo/desbloqueo de tarjetas")
public class CardSecurityController {

    private final BlockCardUseCase blockCardUseCase;
    private final UnblockCardUseCase unblockCardUseCase;
    private final CardDtoMapper mapper;

    /**
     * Bloquear tarjeta
     */
    @PostMapping("/{cardId}/block")
    @Operation(summary = "Bloquear tarjeta", description = "Bloquea una tarjeta temporalmente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarjeta bloqueada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Tarjeta no encontrada")
    })
    public ResponseEntity<CardResponse> blockCard(
            @Parameter(description = "ID de la tarjeta")
            @PathVariable String cardId,

            @Valid @RequestBody BlockCardRequest request
    ) {
        log.info("Blocking card: {} - Reason: {}", cardId, request.getReason());

        BlockCardUseCase.BlockCardCommand command = new BlockCardUseCase.BlockCardCommand(
                CardId.of(cardId),
                request.getReason()
        );

        Card card = blockCardUseCase.execute(command);

        CardResponse response = mapper.toResponse(card);

        log.info("Card blocked successfully: {}", cardId);

        return ResponseEntity.ok(response);
    }

    /**
     * Bloquear tarjeta por fraude
     */
    @PostMapping("/{cardId}/block-fraud")
    @Operation(summary = "Bloquear tarjeta por fraude", description = "Bloquea una tarjeta por sospecha de fraude")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarjeta bloqueada por fraude"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Tarjeta no encontrada")
    })
    public ResponseEntity<CardResponse> blockCardForFraud(
            @Parameter(description = "ID de la tarjeta")
            @PathVariable String cardId,

            @Valid @RequestBody BlockCardRequest request
    ) {
        log.warn("Blocking card for FRAUD: {} - Reason: {}", cardId, request.getReason());

        BlockCardUseCase.BlockCardCommand command = new BlockCardUseCase.BlockCardCommand(
                CardId.of(cardId),
                request.getReason()
        );

        Card card = blockCardUseCase.executeForFraud(command);

        CardResponse response = mapper.toResponse(card);

        log.warn("Card blocked for FRAUD: {}", cardId);

        return ResponseEntity.ok(response);
    }

    /**
     * Desbloquear tarjeta
     */
    @PostMapping("/{cardId}/unblock")
    @Operation(summary = "Desbloquear tarjeta", description = "Desbloquea una tarjeta previamente bloqueada")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarjeta desbloqueada exitosamente"),
            @ApiResponse(responseCode = "400", description = "La tarjeta no puede ser desbloqueada"),
            @ApiResponse(responseCode = "404", description = "Tarjeta no encontrada")
    })
    public ResponseEntity<CardResponse> unblockCard(
            @Parameter(description = "ID de la tarjeta")
            @PathVariable String cardId
    ) {
        log.info("Unblocking card: {}", cardId);

        Card card = unblockCardUseCase.execute(CardId.of(cardId));

        CardResponse response = mapper.toResponse(card);

        log.info("Card unblocked successfully: {}", cardId);

        return ResponseEntity.ok(response);
    }
}
