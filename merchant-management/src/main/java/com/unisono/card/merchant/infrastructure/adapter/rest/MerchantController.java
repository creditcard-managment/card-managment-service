package com.unisono.card.merchant.infrastructure.adapter.rest;

import com.unisono.card.merchant.application.dto.request.RegisterMerchantRequest;
import com.unisono.card.merchant.application.dto.request.UpdateMerchantProfileRequest;
import com.unisono.card.merchant.application.dto.response.MerchantResponse;
import com.unisono.card.merchant.application.dto.response.PageResponse;
import com.unisono.card.merchant.application.port.input.MerchantManagementUseCase;
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

/**
 * REST Controller para gestión de Merchants
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
@Tag(name = "Merchants", description = "Merchant management endpoints")
public class MerchantController {

    private final MerchantManagementUseCase.Register registerUseCase;
    private final MerchantManagementUseCase.Activate activateUseCase;
    private final MerchantManagementUseCase.Suspend suspendUseCase;
    private final MerchantManagementUseCase.UpdateProfile updateProfileUseCase;
    private final MerchantManagementUseCase.FindById findByIdUseCase;
    private final MerchantManagementUseCase.FindAll findAllUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_merchant:write')")
    @Operation(summary = "Register new merchant", description = "Creates a new merchant in the system")
    public ResponseEntity<MerchantResponse> registerMerchant(
            @Valid @RequestBody RegisterMerchantRequest request
    ) {
        log.info("REST request to register new merchant: {}", request.getLegalName());
        MerchantResponse response = registerUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{merchantId}")
    @PreAuthorize("hasAuthority('SCOPE_merchant:read')")
    @Operation(summary = "Get merchant by ID", description = "Retrieves merchant details by ID")
    public ResponseEntity<MerchantResponse> getMerchantById(
            @Parameter(description = "Merchant ID") @PathVariable String merchantId
    ) {
        log.info("REST request to get merchant: {}", merchantId);
        MerchantResponse response = findByIdUseCase.execute(merchantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_merchant:read')")
    @Operation(summary = "Get all merchants", description = "Retrieves paginated list of merchants")
    public ResponseEntity<PageResponse<MerchantResponse>> getAllMerchants(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size
    ) {
        log.info("REST request to get all merchants - page: {}, size: {}", page, size);
        java.util.List<MerchantResponse> merchants = findAllUseCase.execute(page, size);
        PageResponse<MerchantResponse> response = PageResponse.of(merchants, page, size, merchants.size());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{merchantId}/activate")
    @PreAuthorize("hasAuthority('SCOPE_merchant:write')")
    @Operation(summary = "Activate merchant", description = "Activates a pending merchant")
    public ResponseEntity<MerchantResponse> activateMerchant(
            @Parameter(description = "Merchant ID") @PathVariable String merchantId,
            @Parameter(description = "User who activated") @RequestParam(required = false) String activatedBy
    ) {
        log.info("REST request to activate merchant: {}", merchantId);
        MerchantResponse response = activateUseCase.execute(merchantId, activatedBy);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{merchantId}/suspend")
    @PreAuthorize("hasAuthority('SCOPE_merchant:write')")
    @Operation(summary = "Suspend merchant", description = "Suspends an active merchant")
    public ResponseEntity<MerchantResponse> suspendMerchant(
            @Parameter(description = "Merchant ID") @PathVariable String merchantId,
            @Parameter(description = "Suspension reason") @RequestParam String reason,
            @Parameter(description = "User who suspended") @RequestParam(required = false) String suspendedBy
    ) {
        log.info("REST request to suspend merchant: {} - Reason: {}", merchantId, reason);
        MerchantResponse response = suspendUseCase.execute(merchantId, reason, suspendedBy);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{merchantId}/profile")
    @PreAuthorize("hasAuthority('SCOPE_merchant:write')")
    @Operation(summary = "Update merchant profile", description = "Updates merchant contact information")
    public ResponseEntity<MerchantResponse> updateMerchantProfile(
            @Parameter(description = "Merchant ID") @PathVariable String merchantId,
            @Valid @RequestBody UpdateMerchantProfileRequest request
    ) {
        log.info("REST request to update merchant profile: {}", merchantId);
        MerchantResponse response = updateProfileUseCase.execute(merchantId, request);
        return ResponseEntity.ok(response);
    }
}
