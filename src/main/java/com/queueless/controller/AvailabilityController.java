package com.queueless.controller;

import com.queueless.dto.appointment.AvailabilityResponse;
import com.queueless.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/providers/{providerId}/availability")
@Tag(name = "Availability", description = "Endpoints for provider slot availability calculation")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping
    @Operation(summary = "Get provider slot availability", description = "Public endpoint to calculate available appointment time slots for a provider and service on a specific date.")
    public ResponseEntity<AvailabilityResponse> getAvailability(
            @PathVariable Long providerId,
            @RequestParam Long serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        AvailabilityResponse response = availabilityService.getAvailability(providerId, serviceId, date);
        return ResponseEntity.ok(response);
    }
}
