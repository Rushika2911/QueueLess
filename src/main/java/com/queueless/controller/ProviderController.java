package com.queueless.controller;

import com.queueless.dto.provider.CreateProviderRequest;
import com.queueless.dto.provider.ProviderResponse;
import com.queueless.dto.provider.UpdateProviderRequest;
import com.queueless.dto.provider.UpdateProviderStatusRequest;
import com.queueless.entity.enums.ProviderStatus;
import com.queueless.security.CustomUserDetails;
import com.queueless.service.ProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/providers")
@Tag(name = "Providers", description = "Endpoints for provider management and browsing")
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @GetMapping
    @Operation(summary = "Get list of providers", description = "Public endpoint to retrieve paginated list of providers with optional filtering by status and specialization.")
    public ResponseEntity<Page<ProviderResponse>> getAllProviders(
            @RequestParam(required = false) ProviderStatus status,
            @RequestParam(required = false) String specialization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<ProviderResponse> providers = providerService.getAllProviders(status, specialization, pageable);
        return ResponseEntity.ok(providers);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get provider by ID", description = "Public endpoint to retrieve details of a specific provider.")
    public ResponseEntity<ProviderResponse> getProviderById(@PathVariable Long id) {
        ProviderResponse provider = providerService.getProviderById(id);
        return ResponseEntity.ok(provider);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create provider record", description = "ADMIN only. Creates a provider record for an existing provider user.")
    public ResponseEntity<ProviderResponse> createProvider(@Valid @RequestBody CreateProviderRequest request) {
        ProviderResponse provider = providerService.createProvider(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(provider);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    @Operation(summary = "Update provider details", description = "Provider owner or ADMIN. Updates specialization and description.")
    public ResponseEntity<ProviderResponse> updateProvider(
            @PathVariable Long id,
            @RequestBody UpdateProviderRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        ProviderResponse updated = providerService.updateProvider(id, request, currentUser);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update provider status", description = "ADMIN only. Changes provider status to ACTIVE or INACTIVE.")
    public ResponseEntity<ProviderResponse> updateProviderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProviderStatusRequest request
    ) {
        ProviderResponse updated = providerService.updateProviderStatus(id, request.getStatus());
        return ResponseEntity.ok(updated);
    }
}
