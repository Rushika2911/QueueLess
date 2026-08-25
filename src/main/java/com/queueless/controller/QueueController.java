package com.queueless.controller;

import com.queueless.dto.queue.JoinQueueRequest;
import com.queueless.dto.queue.QueueEntryResponse;
import com.queueless.dto.queue.QueuePositionResponse;
import com.queueless.entity.enums.QueueStatus;
import com.queueless.security.CustomUserDetails;
import com.queueless.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/queues")
@Tag(name = "Virtual Queue Engine", description = "Endpoints for remote queue joins, status transitions, position tracking, and provider queue management")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @PostMapping("/{providerId}/join")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Join virtual queue", description = "CUSTOMER only. Joins provider queue for today and receives token & position.")
    public ResponseEntity<QueueEntryResponse> joinQueue(
            @PathVariable Long providerId,
            @RequestBody(required = false) JoinQueueRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        QueueEntryResponse response = queueService.joinQueue(providerId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-position")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Get my queue position", description = "CUSTOMER only. Retrieves live token, position, and estimated wait for current customer.")
    public ResponseEntity<QueuePositionResponse> getMyPosition(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        QueuePositionResponse response = queueService.getMyPosition(currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{providerId}")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    @Operation(summary = "Get provider queue entries", description = "PROVIDER owner or ADMIN. Paginated listing of queue entries for a provider.")
    public ResponseEntity<Page<QueueEntryResponse>> getQueueEntries(
            @PathVariable Long providerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) QueueStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("tokenNumber").ascending());
        Page<QueueEntryResponse> entries = queueService.getQueueEntries(providerId, date, status, pageable, currentUser);
        return ResponseEntity.ok(entries);
    }

    @PostMapping("/{providerId}/next")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    @Operation(summary = "Call next customer", description = "PROVIDER owner only. Transitions earliest WAITING customer to CALLED.")
    public ResponseEntity<QueueEntryResponse> callNext(
            @PathVariable Long providerId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        QueueEntryResponse response = queueService.callNext(providerId, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{entryId}/serve")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    @Operation(summary = "Start serving customer", description = "PROVIDER owner only. Transitions CALLED customer to SERVING.")
    public ResponseEntity<QueueEntryResponse> startServing(
            @PathVariable Long entryId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        QueueEntryResponse response = queueService.startServing(entryId, currentUser);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{entryId}/complete")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    @Operation(summary = "Complete queue entry", description = "PROVIDER owner only. Transitions SERVING customer to COMPLETED.")
    public ResponseEntity<QueueEntryResponse> completeQueueEntry(
            @PathVariable Long entryId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        QueueEntryResponse response = queueService.completeQueueEntry(entryId, currentUser);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{entryId}/no-show")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    @Operation(summary = "Mark customer as no-show", description = "PROVIDER owner only. Transitions CALLED customer to NO_SHOW.")
    public ResponseEntity<QueueEntryResponse> markNoShow(
            @PathVariable Long entryId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        QueueEntryResponse response = queueService.markNoShow(entryId, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{entryId}/leave")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Leave queue", description = "CUSTOMER owner only. Leaves a WAITING queue entry (status WAITING -> LEFT).")
    public ResponseEntity<QueueEntryResponse> leaveQueue(
            @PathVariable Long entryId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        QueueEntryResponse response = queueService.leaveQueue(entryId, currentUser);
        return ResponseEntity.ok(response);
    }
}
