package com.unisono.card.limits.infrastructure.adapter.input.rest;

import com.unisono.card.limits.application.dto.request.*;
import com.unisono.card.limits.application.dto.response.CreditInfoResponse;
import com.unisono.card.limits.application.dto.response.CreditLimitHistoryResponse;
import com.unisono.card.limits.application.dto.response.CancelOperationResponse;
import com.unisono.card.limits.application.dto.response.CreditLimitResponse;
import com.unisono.card.limits.application.mapper.CreditLimitDtoMapper;
import com.unisono.card.limits.domain.model.CreditLimit;
import com.unisono.card.limits.domain.model.CreditLimitHistory;
import com.unisono.card.limits.domain.port.input.AssignCreditLimitUseCase;
import com.unisono.card.limits.domain.port.input.GetCreditInfoUseCase;
import com.unisono.card.limits.domain.port.input.RecalculateAvailableCreditUseCase;
import com.unisono.card.limits.domain.port.input.UpdateCreditLimitUseCase;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller para gestión de límites de crédito
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/credit-limits")
@RequiredArgsConstructor
@Tag(name = "Credit Limits", description = "Endpoints para gestión de límites de crédito")
public class CreditLimitController {

    private final AssignCreditLimitUseCase assignCreditLimitUseCase;
    private final UpdateCreditLimitUseCase updateCreditLimitUseCase;
    private final RecalculateAvailableCreditUseCase recalculateAvailableCreditUseCase;
    private final GetCreditInfoUseCase getCreditInfoUseCase;
    private final CreditLimitDtoMapper mapper;

    // ========================================
    // ASSIGN CREDIT LIMIT
    // ========================================

    @PostMapping("/cards/{cardId}")
    @Operation(summary = "Asignar límite de crédito",
            description = "Asigna límites de crédito a una tarjeta")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Límite asignado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "409", description = "La tarjeta ya tiene límites asignados")
    })
    public ResponseEntity<CreditLimitResponse> assignCreditLimit(
            @Parameter(description = "ID de la tarjeta")
            @PathVariable String cardId,
            @Valid @RequestBody AssignCreditLimitRequest request
    ) {
        log.info("Assigning credit limit to card: {}", cardId);

        // Construir el command
        AssignCreditLimitUseCase.AssignCreditLimitCommand command =
                new AssignCreditLimitUseCase.AssignCreditLimitCommand(
                        CardId.of(cardId),
                        Money.of(request.getPurchaseLimitAmount(), request.getCurrency()),
                        request.getCashAdvancePercentage(),
                        request.getDailyLimitAmount() != null
                                ? Money.of(request.getDailyLimitAmount(), request.getCurrency())
                                : null,
                        request.getMonthlyLimitAmount() != null
                                ? Money.of(request.getMonthlyLimitAmount(), request.getCurrency())
                                : null,
                        request.getAssignedBy()
                );

        // Ejecutar use case
        CreditLimit creditLimit = assignCreditLimitUseCase.execute(command);

        // Convertir a response
        CreditLimitResponse response = mapper.toResponse(creditLimit);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping
    @Operation(summary = "Asignar límite de crédito",
            description = "Asigna un nuevo límite de crédito a una tarjeta")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Límite asignado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Tarjeta no encontrada"),
            @ApiResponse(responseCode = "409", description = "La tarjeta ya tiene límite asignado")
    })
    public ResponseEntity<CreditLimitResponse> assignCreditLimit(
            @Valid @RequestBody AssignCreditLimitRequest request
    ) {
        log.info("Assigning credit limit to card: {}", request.getCardId());

        AssignCreditLimitUseCase.AssignCreditLimitCommand command = mapper.toCommand(request);
        CreditLimit creditLimit = assignCreditLimitUseCase.execute(command);

        CreditLimitResponse response = mapper.toResponse(creditLimit);

        log.info("Credit limit assigned successfully: {}", creditLimit.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ========================================
    // GET CREDIT LIMIT
    // ========================================

    @GetMapping("/{creditLimitId}")
    @Operation(summary = "Obtener límite por ID",
            description = "Obtiene información detallada de un límite de crédito")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Límite encontrado"),
            @ApiResponse(responseCode = "404", description = "Límite no encontrado")
    })
    public ResponseEntity<CreditLimitResponse> getCreditLimitById(
            @Parameter(description = "ID del límite de crédito")
            @PathVariable String creditLimitId
    ) {
        log.info("Getting credit limit by ID: {}", creditLimitId);

        CreditLimit creditLimit = getCreditInfoUseCase.findById(creditLimitId)
                .orElseThrow(() -> new NotFoundException("CreditLimit", creditLimitId));

        CreditLimitResponse response = mapper.toResponse(creditLimit);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/card/{cardId}")
    @Operation(summary = "Obtener límite por tarjeta",
            description = "Obtiene el límite de crédito de una tarjeta específica")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Límite encontrado"),
            @ApiResponse(responseCode = "404", description = "Límite no encontrado para esta tarjeta")
    })
    public ResponseEntity<CreditLimitResponse> getCreditLimitByCard(
            @Parameter(description = "ID de la tarjeta")
            @PathVariable String cardId
    ) {
        log.info("Getting credit limit for card: {}", cardId);

        CreditLimit creditLimit = getCreditInfoUseCase.findByCardId(CardId.of(cardId))
                .orElseThrow(() -> new NotFoundException("CreditLimit", "for card " + cardId));

        CreditLimitResponse response = mapper.toResponse(creditLimit);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/card/{cardId}/info")
    @Operation(summary = "Obtener información de crédito",
            description = "Obtiene información detallada del crédito disponible")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Información obtenida"),
            @ApiResponse(responseCode = "404", description = "Límite no encontrado")
    })
    public ResponseEntity<CreditInfoResponse> getCreditInfo(
            @Parameter(description = "ID de la tarjeta")
            @PathVariable String cardId
    ) {
        log.info("Getting credit info for card: {}", cardId);

        CreditLimit creditLimit = getCreditInfoUseCase.findByCardId(CardId.of(cardId))
                .orElseThrow(() -> new NotFoundException("CreditLimit", "for card " + cardId));

        CreditInfoResponse response = mapper.toCreditInfoResponse(creditLimit);

        return ResponseEntity.ok(response);
    }

    // ========================================
    // UPDATE LIMITS
    // ========================================

    @PutMapping("/{creditLimitId}/increase-purchase-limit")
    @Operation(summary = "Aumentar límite de compras",
            description = "Incrementa el límite de compras")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Límite aumentado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Límite no encontrado")
    })
    public ResponseEntity<CreditLimitResponse> increasePurchaseLimit(
            @Parameter(description = "ID del límite de crédito")
            @PathVariable String creditLimitId,

            @Valid @RequestBody UpdateCreditLimitRequest request
    ) {
        log.info("Increasing purchase limit: {}", creditLimitId);

        UpdateCreditLimitUseCase.IncreasePurchaseLimitCommand command =
                mapper.toIncreaseCommand(creditLimitId, request);

        CreditLimit creditLimit = updateCreditLimitUseCase.increasePurchaseLimit(command);

        CreditLimitResponse response = mapper.toResponse(creditLimit);

        log.info("Purchase limit increased successfully");

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{creditLimitId}/decrease-purchase-limit")
    @Operation(summary = "Disminuir límite de compras",
            description = "Reduce el límite de compras")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Límite reducido exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o límite menor al consumido"),
            @ApiResponse(responseCode = "404", description = "Límite no encontrado")
    })
    public ResponseEntity<CreditLimitResponse> decreasePurchaseLimit(
            @Parameter(description = "ID del límite de crédito")
            @PathVariable String creditLimitId,

            @Valid @RequestBody UpdateCreditLimitRequest request
    ) {
        log.info("Decreasing purchase limit: {}", creditLimitId);

        UpdateCreditLimitUseCase.DecreasePurchaseLimitCommand command =
                mapper.toDecreaseCommand(creditLimitId, request);

        CreditLimit creditLimit = updateCreditLimitUseCase.decreasePurchaseLimit(command);

        CreditLimitResponse response = mapper.toResponse(creditLimit);

        log.info("Purchase limit decreased successfully");

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{creditLimitId}/update-cash-advance-limit")
    @Operation(summary = "Actualizar límite de adelantos",
            description = "Modifica el límite de adelantos en efectivo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Límite actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Límite excede el límite de compras"),
            @ApiResponse(responseCode = "404", description = "Límite no encontrado")
    })
    public ResponseEntity<CreditLimitResponse> updateCashAdvanceLimit(
            @Parameter(description = "ID del límite de crédito")
            @PathVariable String creditLimitId,

            @Valid @RequestBody UpdateCreditLimitRequest request
    ) {
        log.info("Updating cash advance limit: {}", creditLimitId);

        UpdateCreditLimitUseCase.UpdateCashAdvanceLimitCommand command =
                mapper.toCashAdvanceCommand(creditLimitId, request);

        CreditLimit creditLimit = updateCreditLimitUseCase.updateCashAdvanceLimit(command);

        CreditLimitResponse response = mapper.toResponse(creditLimit);

        log.info("Cash advance limit updated successfully");

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{creditLimitId}/update-temporal-limits")
    @Operation(summary = "Actualizar límites temporales",
            description = "Modifica los límites diarios y/o mensuales")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Límites actualizados exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Límite no encontrado")
    })
    public ResponseEntity<CreditLimitResponse> updateTemporalLimits(
            @Parameter(description = "ID del límite de crédito")
            @PathVariable String creditLimitId,

            @Valid @RequestBody UpdateTemporalLimitsRequest request
    ) {
        log.info("Updating temporal limits: {}", creditLimitId);

        UpdateCreditLimitUseCase.UpdateTemporalLimitsCommand command =
                mapper.toTemporalCommand(creditLimitId, request);

        CreditLimit creditLimit = updateCreditLimitUseCase.updateTemporalLimits(command);

        CreditLimitResponse response = mapper.toResponse(creditLimit);

        log.info("Temporal limits updated successfully");

        return ResponseEntity.ok(response);
    }

    // ========================================
    // CONSUME & RELEASE CREDIT
    // ========================================

    @PostMapping("/{creditLimitId}/consume-purchase")
    @Operation(summary = "Consumir crédito (compra)",
            description = "Registra una compra y consume el crédito correspondiente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Crédito consumido exitosamente"),
            @ApiResponse(responseCode = "400", description = "Crédito insuficiente o límite excedido"),
            @ApiResponse(responseCode = "404", description = "Límite no encontrado")
    })
    public ResponseEntity<CreditLimitResponse> consumeForPurchase(
            @Parameter(description = "ID del límite de crédito")
            @PathVariable String creditLimitId,

            @Valid @RequestBody ConsumeCreditRequest request
    ) {
        log.info("Consuming credit for purchase: {} - Amount: {}",
                creditLimitId, request.getAmount());

        RecalculateAvailableCreditUseCase.ConsumeCreditCommand command =
                mapper.toConsumeCommand(creditLimitId, request);

        CreditLimit creditLimit = recalculateAvailableCreditUseCase.consumeForPurchase(command);

        CreditLimitResponse response = mapper.toResponse(creditLimit);

        log.info("Credit consumed successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{creditLimitId}/consume-cash-advance")
    @Operation(summary = "Consumir crédito (adelanto)",
            description = "Registra un adelanto en efectivo y consume el crédito")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Crédito consumido exitosamente"),
            @ApiResponse(responseCode = "400", description = "Crédito insuficiente o límite excedido"),
            @ApiResponse(responseCode = "404", description = "Límite no encontrado")
    })
    public ResponseEntity<CreditLimitResponse> consumeForCashAdvance(
            @Parameter(description = "ID del límite de crédito")
            @PathVariable String creditLimitId,

            @Valid @RequestBody ConsumeCreditRequest request
    ) {
        log.info("Consuming credit for cash advance: {} - Amount: {}",
                creditLimitId, request.getAmount());

        RecalculateAvailableCreditUseCase.ConsumeCreditCommand command =
                mapper.toConsumeCommand(creditLimitId, request);

        CreditLimit creditLimit = recalculateAvailableCreditUseCase.consumeForCashAdvance(command);

        CreditLimitResponse response = mapper.toResponse(creditLimit);

        log.info("Credit consumed successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{creditLimitId}/release-credit")
    @Operation(summary = "Liberar crédito (pago)",
            description = "Registra un pago y libera el crédito correspondiente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Crédito liberado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Límite no encontrado")
    })
    public ResponseEntity<CreditLimitResponse> releaseCredit(
            @Parameter(description = "ID del límite de crédito")
            @PathVariable String creditLimitId,

            @Valid @RequestBody ReleaseCreditRequest request
    ) {
        log.info("Releasing credit: {} - Payment: {}",
                creditLimitId, request.getPaymentAmount());

        RecalculateAvailableCreditUseCase.ReleaseCreditCommand command =
                mapper.toReleaseCommand(creditLimitId, request);

        CreditLimit creditLimit = recalculateAvailableCreditUseCase.releaseCredit(command);

        CreditLimitResponse response = mapper.toResponse(creditLimit);

        log.info("Credit released successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{creditLimitId}/restore-credit")
    @Operation(summary = "Restaurar crédito (reverso o devolución)",
            description = "Deshace el consumo de una transacción reversada o devuelta: sube el disponible y "
                    + "devuelve el cupo diario y mensual si la transacción original es del día o del mes en curso")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Crédito restaurado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Límite no encontrado")
    })
    public ResponseEntity<CreditLimitResponse> restoreCredit(
            @Parameter(description = "ID del límite de crédito")
            @PathVariable String creditLimitId,

            @Valid @RequestBody RestoreCreditRequest request
    ) {
        log.info("Restoring credit: {} - Amount: {}", creditLimitId, request.getAmount());

        CreditLimit creditLimit = recalculateAvailableCreditUseCase.restoreCredit(
                mapper.toRestoreCommand(creditLimitId, request));

        return ResponseEntity.ok(mapper.toResponse(creditLimit));
    }

    @PostMapping("/{creditLimitId}/operations/{operationId}/cancel")
    @Operation(summary = "Cancelar una operación de crédito",
            description = "Deshace exactamente un consumo o una restauración enviados con operationId. "
                    + "Si la operación todavía no llegó, la anula y se rechaza cuando llegue. Es idempotente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operación cancelada, anulada o ya cancelada"),
            @ApiResponse(responseCode = "404", description = "Límite no encontrado"),
            @ApiResponse(responseCode = "422", description = "La operación pertenece a otro límite")
    })
    public ResponseEntity<CancelOperationResponse> cancelOperation(
            @Parameter(description = "ID del límite de crédito")
            @PathVariable String creditLimitId,

            @Parameter(description = "Id de la operación")
            @PathVariable String operationId
    ) {
        log.info("Cancelling credit operation {} of {}", operationId, creditLimitId);

        RecalculateAvailableCreditUseCase.CancelOperationResult result = recalculateAvailableCreditUseCase.cancelOperation(
                new RecalculateAvailableCreditUseCase.CancelOperationCommand(creditLimitId, operationId));

        CreditLimit creditLimit = result.creditLimit();
        return ResponseEntity.ok(CancelOperationResponse.builder()
                .operationId(operationId)
                .outcome(result.outcome().name())
                .availableCredit(creditLimit.getAvailableCredit().getAmount().getAmount())
                .currency(creditLimit.getAvailableCredit().getAmount().getCurrencyCode())
                .overLimitAmount(creditLimit.getOverLimitAmount().getAmount())
                .build());
    }

    // ========================================
    // HISTORY
    // ========================================

    @GetMapping("/{creditLimitId}/history")
    @Operation(summary = "Obtener historial de cambios",
            description = "Retorna el historial completo de modificaciones del límite")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial obtenido"),
            @ApiResponse(responseCode = "404", description = "Límite no encontrado")
    })
    public ResponseEntity<List<CreditLimitHistoryResponse>> getHistory(
            @Parameter(description = "ID del límite de crédito")
            @PathVariable String creditLimitId
    ) {
        log.info("Getting history for credit limit: {}", creditLimitId);

        List<CreditLimitHistory> history = getCreditInfoUseCase.getHistory(creditLimitId);

        List<CreditLimitHistoryResponse> response = history.stream()
                .map(mapper::toHistoryResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
