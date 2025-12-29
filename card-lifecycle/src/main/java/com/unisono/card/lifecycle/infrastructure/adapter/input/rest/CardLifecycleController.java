package com.unisono.card.lifecycle.infrastructure.adapter.input.rest;

import com.unisono.card.lifecycle.application.service.CardLifecycleService;
import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.lifecycle.infrastructure.adapter.input.rest.dto.*;
import com.unisono.card.lifecycle.infrastructure.adapter.input.rest.mapper.LifecycleDtoMapper;
import com.unisono.card.shared.domain.valueobject.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/card-lifecycle")
@RequiredArgsConstructor
@Tag(name = "Card Lifecycle", description = "Gestión del ciclo de vida de tarjetas")
public class CardLifecycleController {

    private final CardLifecycleService lifecycleService;
    private final LifecycleDtoMapper mapper;

    // =============================================
    // ENDPOINTS DE TRANSICIÓN DE ESTADO
    // =============================================

    @PostMapping("/{cardId}/approve")
    @Operation(summary = "Aprobar tarjeta", description = "Aprueba una tarjeta solicitada (REQUESTED → APPROVED)")
    public ResponseEntity<Void> approveCard(
            @PathVariable String cardId,
            @Valid @RequestBody ApproveCardRequest request
    ) {
        log.info("REST: Aprobando tarjeta: {}", cardId);

        lifecycleService.approveCard(
                CardId.of(cardId),
                request.getReason(),
                request.getApprovedBy()
        );

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{cardId}/reject")
    @Operation(summary = "Rechazar tarjeta", description = "Rechaza una tarjeta solicitada (REQUESTED → REJECTED)")
    public ResponseEntity<Void> rejectCard(
            @PathVariable String cardId,
            @Valid @RequestBody RejectCardRequest request
    ) {
        log.info("REST: Rechazando tarjeta: {}", cardId);

        lifecycleService.rejectCard(
                CardId.of(cardId),
                request.getReason(),
                request.getRejectedBy()
        );

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{cardId}/issue")
    @Operation(summary = "Emitir tarjeta", description = "Emite una tarjeta aprobada asignando número, CVV, PIN y fecha de expiración (APPROVED → ISSUED)")
    public ResponseEntity<Void> issueCard(
            @PathVariable String cardId,
            @Valid @RequestBody IssueCardRequest request
    ) {
        log.info("REST: Emitiendo tarjeta: {}", cardId);

        // ✅ Convertir DTO → Value Objects directamente
        lifecycleService.issueCard(
                CardId.of(cardId),
                CardNumber.of(request.getCardNumber()),
                CVV.of(request.getCvv()),
                hashPin(request.getPin()), // TODO: Implementar encriptación real
                ExpirationDate.of(request.getExpirationMonth(), request.getExpirationYear()),
                request.getReason(),
                request.getIssuedBy()
        );

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{cardId}/activate")
    @Operation(summary = "Activar tarjeta", description = "Activa una tarjeta emitida (ISSUED → ACTIVE)")
    public ResponseEntity<Void> activateCard(
            @PathVariable String cardId,
            @Valid @RequestBody ActivateCardRequest request
    ) {
        log.info("REST: Activando tarjeta: {}", cardId);

        lifecycleService.activateCard(
                CardId.of(cardId),
                request.getReason(),
                request.getActivatedBy()
        );

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{cardId}/block")
    @Operation(summary = "Bloquear tarjeta", description = "Bloquea una tarjeta activa (ACTIVE → BLOCKED)")
    public ResponseEntity<Void> blockCard(
            @PathVariable String cardId,
            @Valid @RequestBody BlockCardRequest request
    ) {
        log.info("REST: Bloqueando tarjeta: {}", cardId);

        if ("FRAUD".equalsIgnoreCase(request.getBlockType())) {
            lifecycleService.blockCardForFraud(
                    CardId.of(cardId),
                    request.getReason(),
                    request.getBlockedBy()
            );
        } else {
            lifecycleService.blockCard(
                    CardId.of(cardId),
                    request.getReason(),
                    request.getBlockedBy()
            );
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{cardId}/cancel")
    @Operation(summary = "Cancelar tarjeta", description = "Cancela una tarjeta (ANY → CANCELLED)")
    public ResponseEntity<Void> cancelCard(
            @PathVariable String cardId,
            @Valid @RequestBody CancelCardRequest request
    ) {
        log.info("REST: Cancelando tarjeta: {}", cardId);

        lifecycleService.cancelCard(
                CardId.of(cardId),
                request.getReason(),
                request.getCancelledBy()
        );

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
