package com.unisono.card.data.infrastructure.adapter.input.rest;

import com.unisono.card.data.application.dto.request.CreateCardRequest;
import com.unisono.card.data.application.dto.response.CardDetailResponse;
import com.unisono.card.data.application.dto.response.CardResponse;
import com.unisono.card.data.application.mapper.CardDtoMapper;
import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.port.input.CreateCardUseCase;
import com.unisono.card.data.domain.port.input.GetCardUseCase;
import com.unisono.card.data.domain.service.CardManagementService;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.ClientId;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller para gestión de tarjetas
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Endpoints para gestión de tarjetas")
public class CardController {

    private final CardManagementService cardManagementService;
    private final CreateCardUseCase createCardUseCase;
    private final GetCardUseCase getCardUseCase;
    private final CardDtoMapper mapper;

    /**
     * Crear una nueva tarjeta
     */
    @PostMapping
    @Operation(summary = "Crear nueva tarjeta", description = "Crea una solicitud de tarjeta en estado REQUESTED")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tarjeta creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<CardResponse> createCard(
            @Valid @RequestBody CreateCardRequest request
    ) {
        log.info("Creating card for client: {}", request.getClientId());

        CreateCardUseCase.CreateCardCommand command = mapper.toCommand(request);
        Card card = createCardUseCase.execute(command);

        CardResponse response = mapper.toResponse(card);

        log.info("Card created successfully: {}", card.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtener tarjeta por ID
     */
    @GetMapping("/{cardId}")
    @Operation(summary = "Obtener tarjeta por ID", description = "Retorna información detallada de una tarjeta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarjeta encontrada"),
            @ApiResponse(responseCode = "404", description = "Tarjeta no encontrada")
    })
    public ResponseEntity<CardDetailResponse> getCardById(
            @Parameter(description = "ID de la tarjeta")
            @PathVariable String cardId
    ) {
        log.info("Getting card by ID: {}", cardId);

        Card card = getCardUseCase.findById(CardId.of(cardId))
                .orElseThrow(() -> new NotFoundException("Card", cardId));

        CardDetailResponse response = mapper.toDetailResponse(card);

        return ResponseEntity.ok(response);
    }

    /**
     * Obtener todas las tarjetas de un cliente
     */
    @GetMapping("/client/{clientId}")
    @Operation(summary = "Obtener tarjetas por cliente", description = "Retorna todas las tarjetas de un cliente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarjetas encontradas"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<List<CardResponse>> getCardsByClient(
            @Parameter(description = "ID del cliente")
            @PathVariable String clientId
    ) {
        log.info("Getting cards for client: {}", clientId);

        List<Card> cards = getCardUseCase.findByClientId(ClientId.of(clientId));

        List<CardResponse> response = cards.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Buscar tarjeta por últimos 4 dígitos
     */
    @GetMapping("/search")
    @Operation(summary = "Buscar tarjeta por últimos 4 dígitos",
            description = "Retorna una tarjeta específica del cliente por sus últimos 4 dígitos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarjeta encontrada"),
            @ApiResponse(responseCode = "404", description = "Tarjeta no encontrada")
    })
    public ResponseEntity<CardDetailResponse> searchCard(
            @Parameter(description = "Últimos 4 dígitos de la tarjeta", example = "1234")
            @RequestParam String lastFourDigits,

            @Parameter(description = "ID del cliente")
            @RequestParam String clientId
    ) {
        log.info("Searching card with last 4 digits: {} for client: {}", lastFourDigits, clientId);

        Card card = getCardUseCase.findByLastFourDigits(lastFourDigits, ClientId.of(clientId))
                .orElseThrow(() -> new NotFoundException(
                        "Card",
                        String.format("with last 4 digits %s for client %s", lastFourDigits, clientId)
                ));

        CardDetailResponse response = mapper.toDetailResponse(card);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{cardId}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable String cardId,
            @RequestBody Map<String, String> request) {

        log.info("Actualizando estado de tarjeta: {} -> {}", cardId, request.get("status"));

        // ✅ Usar el servicio existente
        cardManagementService.updateStatus(cardId, request.get("status"));

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{cardId}/activated-at")
    public ResponseEntity<Void> updateActivatedAt(
            @PathVariable String cardId,
            @RequestBody Map<String, String> request) {

        log.info("Actualizando fecha de activación: {}", cardId);

        LocalDateTime activatedAt = LocalDateTime.parse(request.get("activatedAt"));

        // ✅ Usar el servicio existente
        cardManagementService.updateActivatedAt(cardId, activatedAt);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{cardId}/card-data")
    public ResponseEntity<Void> updateCardData(
            @PathVariable String cardId,
            @RequestBody Map<String, Object> request) {

        log.info("Actualizando datos de tarjeta: {}", cardId);

        // ✅ Usar el servicio existente
        cardManagementService.updateCardData(
                cardId,
                (String) request.get("cardNumber"),
                (String) request.get("cvv"),
                (String) request.get("pin"),
                (Integer) request.get("expirationMonth"),
                (Integer) request.get("expirationYear")
        );

        return ResponseEntity.ok().build();
    }
}
