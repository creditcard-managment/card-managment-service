package com.unisono.card.merchant.infrastructure.adapter.rest;

import com.unisono.card.merchant.application.dto.request.ProvisionTerminalRequest;
import com.unisono.card.merchant.application.dto.request.UpdateTerminalConfigurationRequest;
import com.unisono.card.merchant.application.dto.response.TerminalResponse;
import com.unisono.card.merchant.application.port.input.TerminalManagementUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller para gestión de Terminales
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/terminals")
@RequiredArgsConstructor
@Tag(name = "Terminals", description = "Payment terminal management endpoints")
public class TerminalController {

    private final TerminalManagementUseCase.Provision provisionUseCase;
    private final TerminalManagementUseCase.Activate activateUseCase;
    private final TerminalManagementUseCase.Deactivate deactivateUseCase;
    private final TerminalManagementUseCase.FindById findByIdUseCase;
    private final TerminalManagementUseCase.FindByMerchant findByMerchantUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_terminal:write')")
    @Operation(summary = "Provision new terminal", description = "Creates and provisions a new payment terminal")
    public ResponseEntity<TerminalResponse> provisionTerminal(
            @Valid @RequestBody ProvisionTerminalRequest request
    ) {
        log.info("REST request to provision terminal for merchant: {}", request.getMerchantId());
        TerminalResponse response = provisionUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{terminalId}")
    @PreAuthorize("hasAuthority('SCOPE_terminal:read')")
    @Operation(summary = "Get terminal by ID", description = "Retrieves terminal details by ID")
    public ResponseEntity<TerminalResponse> getTerminalById(
            @Parameter(description = "Terminal ID") @PathVariable String terminalId
    ) {
        log.info("REST request to get terminal: {}", terminalId);
        TerminalResponse response = findByIdUseCase.execute(terminalId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/merchant/{merchantId}")
    @PreAuthorize("hasAuthority('SCOPE_terminal:read')")
    @Operation(summary = "Get terminals by merchant", description = "Retrieves all terminals for a specific merchant")
    public ResponseEntity<List<TerminalResponse>> getTerminalsByMerchant(
            @Parameter(description = "Merchant ID") @PathVariable String merchantId
    ) {
        log.info("REST request to get terminals for merchant: {}", merchantId);
        List<TerminalResponse> response = findByMerchantUseCase.execute(merchantId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{terminalId}/activate")
    @PreAuthorize("hasAuthority('SCOPE_terminal:write')")
    @Operation(summary = "Activate terminal", description = "Activates an inactive terminal")
    public ResponseEntity<TerminalResponse> activateTerminal(
            @Parameter(description = "Terminal ID") @PathVariable String terminalId
    ) {
        log.info("REST request to activate terminal: {}", terminalId);
        TerminalResponse response = activateUseCase.execute(terminalId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{terminalId}/deactivate")
    @PreAuthorize("hasAuthority('SCOPE_terminal:write')")
    @Operation(summary = "Deactivate terminal", description = "Deactivates an active terminal")
    public ResponseEntity<TerminalResponse> deactivateTerminal(
            @Parameter(description = "Terminal ID") @PathVariable String terminalId,
            @Parameter(description = "Deactivation reason") @RequestParam(required = false) String reason
    ) {
        log.info("REST request to deactivate terminal: {} - Reason: {}", terminalId, reason);
        TerminalResponse response = deactivateUseCase.execute(terminalId, reason);
        return ResponseEntity.ok(response);
    }
}
