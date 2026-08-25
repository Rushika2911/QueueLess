package com.queueless.controller;

import com.queueless.dto.appointment.AppointmentResponse;
import com.queueless.dto.appointment.CreateAppointmentRequest;
import com.queueless.entity.enums.AppointmentStatus;
import com.queueless.security.CustomUserDetails;
import com.queueless.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/appointments")
@Tag(name = "Appointments", description = "Endpoints for appointment booking, viewing, and cancellation")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Book appointment", description = "Customer or Admin. Books an appointment ensuring no double booking and adhering to provider working hours.")
    public ResponseEntity<AppointmentResponse> bookAppointment(
            @Valid @RequestBody CreateAppointmentRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        AppointmentResponse response = appointmentService.bookAppointment(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get user appointments", description = "Returns paginated appointments for authenticated customer, provider owner, or admin.")
    public ResponseEntity<Page<AppointmentResponse>> getAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("appointmentDate").descending().and(Sort.by("startTime").descending()));
        Page<AppointmentResponse> appointments = appointmentService.getAppointments(date, status, pageable, currentUser);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get appointment details", description = "Returns appointment details if user is customer owner, provider owner, or admin.")
    public ResponseEntity<AppointmentResponse> getAppointmentById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        AppointmentResponse response = appointmentService.getAppointmentById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel appointment", description = "Cancels appointment if within 30 minutes before start time rule.")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        AppointmentResponse response = appointmentService.cancelAppointment(id, currentUser);
        return ResponseEntity.ok(response);
    }
}
