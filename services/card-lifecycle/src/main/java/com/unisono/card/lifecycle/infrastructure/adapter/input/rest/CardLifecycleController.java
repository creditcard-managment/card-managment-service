package com.unisono.card.lifecycle.infrastructure.adapter.input.rest;

import com.unisono.card.lifecycle.application.service.CardLifecycleService;
import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.lifecycle.domain.port.input.ActivateCardUseCase;
import com.unisono.card.lifecycle.domain.port.input.ApproveCardUseCase;
import com.unisono.card.lifecycle.domain.port.input.BlockCardUseCase;
import com.unisono.card.lifecycle.domain.port.input.CancelCardUseCase;
import com.unisono.card.lifecycle.domain.port.input.IssueCardUseCase;
import com.unisono.card.lifecycle.domain.port.input.RejectCardUseCase;
import com.unisono.card.lifecycle.infrastructure.adapter.input.rest.dto.*;
import com.unisono.card.lifecycle.infrastructure.adapter.input.rest.mapper.LifecycleDtoMapper;
import com.unisono.card.shared.domain.valueobject.*;
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

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/card-lifecycle")
@RequiredArgsConstructor
@Tag(name = "Card Lifecycle", description = "Gestión del ciclo de vida de tarjetas")
public class CardLifecycleController {

    private final CardLifecycleService lifecycleService;
    private final LifecycleDtoMapper mapper;
    private final IssueCardUseCase issueCardUseCase;
    private final ApproveCardUseCase approveCardUseCase;
    private final ActivateCardUseCase activateCardUseCase;
    private final RejectCardUseCase rejectCardUseCase;
    private final BlockCardUseCase blockCardUseCase;
    private final CancelCardUseCase cancelCardUseCase;

    // =============================================
    // ENDPOINTS DE TRANSICIÓN DE ESTADO
    // =============================================

    @PostMapping("/{cardId}/approve")
    @Operation(summary = "Aprobar tarjeta", description = "Aprueba una solicitud de tarjeta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarjeta aprobada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Tarjeta no encontrada"),
            @ApiResponse(responseCode = "422", description = "Transición no permitida desde el estado actual")
    })
    public ResponseEntity<CardLifecycleResponse> approveCard(
            @Parameter(description = "ID de la tarjeta") @PathVariable String cardId,
            @Valid @RequestBody ApproveCardRequest request) {

        log.info("REST: Aprobando tarjeta: {}", cardId);

        // ✅ Crear Command y ejecutar
        ApproveCardUseCase.ApproveCardCommand command =
                new ApproveCardUseCase.ApproveCardCommand(
                        CardId.of(cardId),
                        request.getReason(),
                        request.getApprovedBy()
                );

        approveCardUseCase.execute(command);

        // Construir respuesta
        CardLifecycleResponse response = CardLifecycleResponse.builder()
                .cardId(cardId)
                .message("Tarjeta aprobada exitosamente")
                .status("APPROVED")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{cardId}/reject")
    @Operation(summary = "Rechazar tarjeta", description = "Rechaza una tarjeta solicitada (REQUESTED → REJECTED)")
    public ResponseEntity<Void> rejectCard(
            @PathVariable String cardId,
            @Valid @RequestBody RejectCardRequest request
    ) {
        log.info("REST: Rechazando tarjeta: {}", cardId);

        rejectCardUseCase.execute(new RejectCardUseCase.RejectCardCommand(
                CardId.of(cardId),
                request.getReason(),
                request.getRejectedBy()
        ));

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{cardId}/issue")
    @Operation(summary = "Emitir tarjeta", description = "Asigna PAN, CVV, PIN y vencimiento (APPROVED → ISSUED)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarjeta emitida exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Tarjeta no encontrada"),
            @ApiResponse(responseCode = "422", description = "Transición no permitida o rechazada por un guard"),
            @ApiResponse(responseCode = "502", description = "card-data no disponible o con error inesperado")
    })
    public ResponseEntity<CardLifecycleResponse> issueCard(
            @PathVariable String cardId,
            @Valid @RequestBody IssueCardRequest request) {

        log.info("REST: Emitiendo tarjeta: {}", cardId);

        CardNumber cardNumber = CardNumber.of(request.getCardNumber());
        CVV cvv = CVV.of(request.getCvv());
        PIN pin = PIN.of(request.getPin());
        ExpirationDate expirationDate = ExpirationDate.forIssuance(
                request.getExpirationMonth(),
                request.getExpirationYear()
        );

        // ✅ Crear Command
        IssueCardUseCase.IssueCardCommand command;
        command = new IssueCardUseCase.IssueCardCommand(
                CardId.of(cardId),
                cardNumber,
                cvv,
                pin,
                expirationDate,
                request.getReason(),
                request.getIssuedBy()
        );

        issueCardUseCase.execute(command);

        CardLifecycleResponse response = CardLifecycleResponse.builder()
                .cardId(cardId)
                .message("Tarjeta emitida exitosamente")
                .status("ISSUED")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{cardId}/activate")
    @Operation(summary = "Activar tarjeta",
            description = "Verifica PIN y límite de crédito y activa la tarjeta (ISSUED → ACTIVE)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarjeta activada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Tarjeta no encontrada"),
            @ApiResponse(responseCode = "422", description = "Transición no permitida, sin límite de crédito o PIN incorrecto"),
            @ApiResponse(responseCode = "502", description = "card-data no disponible o con error inesperado")
    })
    public ResponseEntity<CardLifecycleResponse> activateCard(
            @PathVariable String cardId,
            @Valid @RequestBody ActivateCardRequest request) {

        log.info("REST: Activando tarjeta: {}", cardId);

        ActivateCardUseCase.ActivateCardCommand command =
                new ActivateCardUseCase.ActivateCardCommand(
                        CardId.of(cardId),
                        PIN.of(request.getPin()),
                        request.getReason(),
                        request.getActivatedBy()
                );

        activateCardUseCase.execute(command);

        CardLifecycleResponse response = CardLifecycleResponse.builder()
                .cardId(cardId)
                .message("Tarjeta activada exitosamente")
                .status("ACTIVE")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{cardId}/block")
    @Operation(summary = "Bloquear tarjeta", description = "Bloquea una tarjeta activa (ACTIVE → BLOCKED)")
    public ResponseEntity<Void> blockCard(
            @PathVariable String cardId,
            @Valid @RequestBody BlockCardRequest request
    ) {
        log.info("REST: Bloqueando tarjeta: {}", cardId);

        BlockCardUseCase.BlockCardCommand command = new BlockCardUseCase.BlockCardCommand(
                CardId.of(cardId),
                request.getReason(),
                request.getBlockedBy()
        );

        if ("FRAUD".equalsIgnoreCase(request.getBlockType())) {
            blockCardUseCase.executeBlockFraud(command);
        } else {
            blockCardUseCase.executeBlock(command);
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{cardId}/cancel")
    @Operation(summary = "Cancelar tarjeta",
            description = "Cancela una tarjeta aprobada, emitida, activa o bloqueada (→ CANCELLED)")
    public ResponseEntity<Void> cancelCard(
            @PathVariable String cardId,
            @Valid @RequestBody CancelCardRequest request
    ) {
        log.info("REST: Cancelando tarjeta: {}", cardId);

        cancelCardUseCase.execute(new CancelCardUseCase.CancelCardCommand(
                CardId.of(cardId),
                request.getReason(),
                request.getCancelledBy()
        ));

        return ResponseEntity.ok().build();
    }

    // =============================================
    // ENDPOINTS DE CONSULTA
    // =============================================

    @GetMapping("/{cardId}/history")
    @Operation(summary = "Obtener historial", description = "Obtiene el historial completo de transiciones de una tarjeta")
    public ResponseEntity<List<LifecycleHistoryResponse>> getHistory(
            @PathVariable String cardId
    ) {
        log.info("REST: Obteniendo historial de tarjeta: {}", cardId);

        List<StateTransition> transitions = lifecycleService.getHistory(CardId.of(cardId));

        List<LifecycleHistoryResponse> response = mapper.toHistoryResponseList(transitions);

        return ResponseEntity.ok(response);
    }

    // =============================================
    // HELPERS PRIVADOS
    // =============================================

    /**
     * Encripta el PIN (PLACEHOLDER)
     * TODO: Implementar encriptación real con BCrypt o similar
     */
    private String hashPin(String pin) {
        // PLACEHOLDER: En producción usar BCrypt o Argon2
        log.warn("ADVERTENCIA: Usando hash placeholder para PIN. Implementar hash real en producción.");
        return "HASHED_" + pin;
    }
}
