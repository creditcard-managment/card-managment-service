package com.unisono.card.merchant.infrastructure.adapter.rest;

import com.unisono.card.merchant.application.dto.request.UpdateFeeStructureRequest;
import com.unisono.card.merchant.application.dto.response.FeeStructureResponse;

import com.unisono.card.merchant.application.port.input.FeeManagementUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller para gestión de Fees
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/merchants/{merchantId}/fees")
@RequiredArgsConstructor
@Tag(name = "Fee Management", description = "Merchant fee structure management endpoints")
public class FeeManagementController {

    private final FeeManagementUseCase.GetFeeStructure getFeeStructureUseCase;
    private final FeeManagementUseCase.UpdateFeeStructure updateFeeStructureUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_fee:read')")
    @Operation(summary = "Get fee structure", description = "Retrieves current fee structure for a merchant")
    public ResponseEntity<FeeStructureResponse> getFeeStructure(
            @Parameter(description = "Merchant ID") @PathVariable String merchantId
    ) {
        log.info("REST request to get fee structure for merchant: {}", merchantId);
        FeeStructureResponse response = getFeeStructureUseCase.execute(merchantId);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    @PreAuthorize("hasAuthority('SCOPE_fee:write')")
    @Operation(summary = "Update fee structure", description = "Updates fee structure for a merchant")
    public ResponseEntity<FeeStructureResponse> updateFeeStructure(
            @Parameter(description = "Merchant ID") @PathVariable String merchantId,
            @Valid @RequestBody UpdateFeeStructureRequest request
    ) {
        log.info("REST request to update fee structure for merchant: {}", merchantId);
        FeeStructureResponse response = updateFeeStructureUseCase.execute(merchantId, request);
        return ResponseEntity.ok(response);
    }
}
