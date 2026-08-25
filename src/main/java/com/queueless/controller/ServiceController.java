package com.queueless.controller;

import com.queueless.dto.service.ServiceRequest;
import com.queueless.dto.service.ServiceResponse;
import com.queueless.security.CustomUserDetails;
import com.queueless.service.ServiceManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@Tag(name = "Services", description = "Endpoints for managing provider services")
public class ServiceController {

    private final ServiceManagementService serviceManagementService;

    public ServiceController(ServiceManagementService serviceManagementService) {
        this.serviceManagementService = serviceManagementService;
    }

    @GetMapping("/api/providers/{providerId}/services")
    @Operation(summary = "Get services by provider", description = "Public endpoint to retrieve services offered by a provider.")
    public ResponseEntity<List<ServiceResponse>> getServicesByProvider(
            @PathVariable Long providerId,
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        List<ServiceResponse> services = serviceManagementService.getServicesByProvider(providerId, activeOnly);
        return ResponseEntity.ok(services);
    }

    @PostMapping("/api/providers/{providerId}/services")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    @Operation(summary = "Create service for provider", description = "PROVIDER owner or ADMIN. Adds a new service for the provider.")
    public ResponseEntity<ServiceResponse> createService(
            @PathVariable Long providerId,
            @Valid @RequestBody ServiceRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        ServiceResponse service = serviceManagementService.createService(providerId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(service);
    }

    @PutMapping("/api/services/{id}")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    @Operation(summary = "Update service", description = "PROVIDER owner or ADMIN. Updates an existing service.")
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        ServiceResponse updated = serviceManagementService.updateService(id, request, currentUser);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/api/services/{id}")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    @Operation(summary = "Deactivate service", description = "PROVIDER owner or ADMIN. Soft deactivates a service.")
    public ResponseEntity<Void> deleteService(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        serviceManagementService.deleteService(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
