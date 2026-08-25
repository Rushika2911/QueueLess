package com.queueless.controller;

import com.queueless.dto.provider.WorkingHourRequest;
import com.queueless.dto.provider.WorkingHourResponse;
import com.queueless.security.CustomUserDetails;
import com.queueless.service.WorkingHourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/providers/{providerId}/working-hours")
@Tag(name = "Working Hours", description = "Endpoints for provider working hours configuration")
public class WorkingHourController {

    private final WorkingHourService workingHourService;

    public WorkingHourController(WorkingHourService workingHourService) {
        this.workingHourService = workingHourService;
    }

    @GetMapping
    @Operation(summary = "Get working hours", description = "Public endpoint to retrieve working hours of a provider.")
    public ResponseEntity<List<WorkingHourResponse>> getWorkingHours(@PathVariable Long providerId) {
        List<WorkingHourResponse> hours = workingHourService.getWorkingHoursByProvider(providerId);
        return ResponseEntity.ok(hours);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    @Operation(summary = "Update working hours", description = "PROVIDER owner or ADMIN. Configures working hours for the provider.")
    public ResponseEntity<List<WorkingHourResponse>> updateWorkingHours(
            @PathVariable Long providerId,
            @Valid @RequestBody List<WorkingHourRequest> requests,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        List<WorkingHourResponse> updated = workingHourService.updateWorkingHours(providerId, requests, currentUser);
        return ResponseEntity.ok(updated);
    }
}
