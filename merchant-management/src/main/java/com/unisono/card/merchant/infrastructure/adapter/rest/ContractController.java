package com.unisono.card.merchant.infrastructure.adapter.rest;

import com.unisono.card.merchant.application.dto.request.RenewContractRequest;
import com.unisono.card.merchant.application.dto.response.ContractResponse;
import com.unisono.card.merchant.application.port.input.ContractManagementUseCase;
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
 * REST Controller para gestión de Contratos
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/merchants/{merchantId}/contract")
@RequiredArgsConstructor
@Tag(name = "Contract Management", description = "Merchant contract management endpoints")
public class ContractController {

    private final ContractManagementUseCase.GetContract getContractUseCase;
    private final ContractManagementUseCase.RenewContract renewContractUseCase;
    private final ContractManagementUseCase.TerminateContract terminateContractUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_contract:read')")
    @Operation(summary = "Get contract", description = "Retrieves current contract for a merchant")
    public ResponseEntity<ContractResponse> getContract(
            @Parameter(description = "Merchant ID") @PathVariable String merchantId
    ) {
        log.info("REST request to get contract for merchant: {}", merchantId);
        ContractResponse response = getContractUseCase.execute(merchantId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/renew")
    @PreAuthorize("hasAuthority('SCOPE_contract:write')")
    @Operation(summary = "Renew contract", description = "Renews contract for a merchant")
    public ResponseEntity<ContractResponse> renewContract(
            @Parameter(description = "Merchant ID") @PathVariable String merchantId,
            @Valid @RequestBody RenewContractRequest request
    ) {
        log.info("REST request to renew contract for merchant: {}", merchantId);
        ContractResponse response = renewContractUseCase.execute(merchantId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/terminate")
    @PreAuthorize("hasAuthority('SCOPE_contract:write')")
    @Operation(summary = "Terminate contract", description = "Terminates contract for a merchant")
    public ResponseEntity<Void> terminateContract(
            @Parameter(description = "Merchant ID") @PathVariable String merchantId,
            @Parameter(description = "Termination reason") @RequestParam String reason
    ) {
        log.info("REST request to terminate contract for merchant: {} - Reason: {}", merchantId, reason);
        terminateContractUseCase.execute(merchantId, reason);
        return ResponseEntity.noContent().build();
    }
}
