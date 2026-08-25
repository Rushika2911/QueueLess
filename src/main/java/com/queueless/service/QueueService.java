package com.queueless.service;

import com.queueless.dto.queue.JoinQueueRequest;
import com.queueless.dto.queue.QueueEntryResponse;
import com.queueless.dto.queue.QueuePositionResponse;
import com.queueless.entity.Appointment;
import com.queueless.entity.Provider;
import com.queueless.entity.QueueEntry;
import com.queueless.entity.User;
import com.queueless.entity.enums.ProviderStatus;
import com.queueless.entity.enums.QueueStatus;
import com.queueless.exception.InvalidQueueTransitionException;
import com.queueless.exception.QueueConflictException;
import com.queueless.exception.ResourceNotFoundException;
import com.queueless.exception.UnauthorizedResourceAccessException;
import com.queueless.repository.AppointmentRepository;
import com.queueless.repository.ProviderRepository;
import com.queueless.repository.QueueEntryRepository;
import com.queueless.repository.UserRepository;
import com.queueless.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class QueueService {

    private final QueueEntryRepository queueEntryRepository;
    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final ProviderService providerService;

    public QueueService(
            QueueEntryRepository queueEntryRepository,
            ProviderRepository providerRepository,
            UserRepository userRepository,
            AppointmentRepository appointmentRepository,
            ProviderService providerService
    ) {
        this.queueEntryRepository = queueEntryRepository;
        this.providerRepository = providerRepository;
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.providerService = providerService;
    }

    @Transactional
    public QueueEntryResponse joinQueue(Long providerId, JoinQueueRequest request, CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedResourceAccessException("Authentication required to join queue");
        }

        User customer = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + providerId));

        if (provider.getStatus() != ProviderStatus.ACTIVE) {
            throw new QueueConflictException("Provider is inactive and cannot accept queue entries");
        }

        LocalDate today = LocalDate.now();

        // Check if customer already has an active queue entry for provider and today
        List<QueueStatus> activeStatuses = List.of(QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.SERVING);
        if (queueEntryRepository.findByCustomerIdAndProviderIdAndQueueDateAndStatusIn(customer.getId(), providerId, today, activeStatuses).isPresent()) {
            throw new QueueConflictException("Customer already has an active queue entry for this provider today");
        }

        Appointment appointment = null;
        if (request != null && request.getAppointmentId() != null) {
            appointment = appointmentRepository.findById(request.getAppointmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + request.getAppointmentId()));
        }

        // Generate next token number atomically for today
        int nextToken = queueEntryRepository.findTopByProviderIdAndQueueDateOrderByTokenNumberDesc(providerId, today)
                .map(entry -> entry.getTokenNumber() + 1)
                .orElse(1);

        QueueEntry entry = new QueueEntry(
                provider,
                customer,
                appointment,
                today,
                nextToken,
                0,
                QueueStatus.WAITING
        );

        QueueEntry saved = queueEntryRepository.save(entry);

        long peopleAhead = queueEntryRepository.countByProviderIdAndQueueDateAndStatusAndJoinedAtBefore(
                providerId, today, QueueStatus.WAITING, saved.getJoinedAt()
        );

        int position = (int) peopleAhead + 1;
        saved.setPosition(position);
        queueEntryRepository.save(saved);

        int serviceDuration = (appointment != null && appointment.getService() != null)
                ? appointment.getService().getDurationMinutes()
                : 15;
        int estimatedWait = (int) peopleAhead * serviceDuration;

        return QueueEntryResponse.fromEntity(saved, position, estimatedWait);
    }

    @Transactional(readOnly = true)
    public QueuePositionResponse getMyPosition(CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedResourceAccessException("Authentication required");
        }

        LocalDate today = LocalDate.now();
        List<QueueStatus> activeStatuses = List.of(QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.SERVING);

        QueueEntry entry = queueEntryRepository.findByCustomerIdAndQueueDateAndStatusIn(currentUser.getId(), today, activeStatuses)
                .orElseThrow(() -> new ResourceNotFoundException("No active queue entry found for today"));

        long peopleAhead = 0;
        if (entry.getStatus() == QueueStatus.WAITING) {
            peopleAhead = queueEntryRepository.countByProviderIdAndQueueDateAndStatusAndJoinedAtBefore(
                    entry.getProvider().getId(), today, QueueStatus.WAITING, entry.getJoinedAt()
            );
        }

        int position = (int) peopleAhead + 1;
        int serviceDuration = (entry.getAppointment() != null && entry.getAppointment().getService() != null)
                ? entry.getAppointment().getService().getDurationMinutes()
                : 15;

        return new QueuePositionResponse(
                entry.getId(),
                entry.getProvider().getId(),
                entry.getProvider().getUser() != null ? entry.getProvider().getUser().getName() : null,
                entry.getTokenNumber(),
                position,
                (int) peopleAhead,
                (int) peopleAhead * serviceDuration,
                entry.getStatus()
        );
    }

    @Transactional
    public QueueEntryResponse callNext(Long providerId, CustomUserDetails currentUser) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + providerId));

        providerService.checkOwnershipOrAdmin(provider, currentUser);

        LocalDate today = LocalDate.now();

        // Rule: If a customer is currently SERVING, do not call another customer
        List<QueueEntry> servingEntries = queueEntryRepository.findByProviderIdAndQueueDateAndStatus(providerId, today, QueueStatus.SERVING);
        if (!servingEntries.isEmpty()) {
            throw new QueueConflictException("A customer is currently being served. Complete or transition current customer before calling next.");
        }

        QueueEntry nextEntry = queueEntryRepository.findFirstByProviderIdAndQueueDateAndStatusOrderByJoinedAtAsc(providerId, today, QueueStatus.WAITING)
                .orElseThrow(() -> new ResourceNotFoundException("No waiting customers in the queue for provider today"));

        nextEntry.setStatus(QueueStatus.CALLED);
        nextEntry.setCalledAt(LocalDateTime.now());

        QueueEntry saved = queueEntryRepository.save(nextEntry);
        return QueueEntryResponse.fromEntity(saved, 1, 0);
    }

    @Transactional
    public QueueEntryResponse startServing(Long entryId, CustomUserDetails currentUser) {
        QueueEntry entry = queueEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found with id: " + entryId));

        providerService.checkOwnershipOrAdmin(entry.getProvider(), currentUser);

        if (entry.getStatus() != QueueStatus.CALLED) {
            throw new InvalidQueueTransitionException("Cannot start serving queue entry in status: " + entry.getStatus() + ". Entry must be CALLED.");
        }

        // Rule: Only one entry may be SERVING for a provider/date
        List<QueueEntry> servingEntries = queueEntryRepository.findByProviderIdAndQueueDateAndStatus(entry.getProvider().getId(), entry.getQueueDate(), QueueStatus.SERVING);
        if (!servingEntries.isEmpty()) {
            throw new QueueConflictException("Another customer is already SERVING for this provider");
        }

        entry.setStatus(QueueStatus.SERVING);
        QueueEntry saved = queueEntryRepository.save(entry);
        return QueueEntryResponse.fromEntity(saved, 0, 0);
    }

    @Transactional
    public QueueEntryResponse completeQueueEntry(Long entryId, CustomUserDetails currentUser) {
        QueueEntry entry = queueEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found with id: " + entryId));

        providerService.checkOwnershipOrAdmin(entry.getProvider(), currentUser);

        if (entry.getStatus() != QueueStatus.SERVING) {
            throw new InvalidQueueTransitionException("Cannot complete queue entry in status: " + entry.getStatus() + ". Entry must be SERVING.");
        }

        entry.setStatus(QueueStatus.COMPLETED);
        entry.setCompletedAt(LocalDateTime.now());
        QueueEntry saved = queueEntryRepository.save(entry);
        return QueueEntryResponse.fromEntity(saved, 0, 0);
    }

    @Transactional
    public QueueEntryResponse markNoShow(Long entryId, CustomUserDetails currentUser) {
        QueueEntry entry = queueEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found with id: " + entryId));

        providerService.checkOwnershipOrAdmin(entry.getProvider(), currentUser);

        if (entry.getStatus() != QueueStatus.CALLED) {
            throw new InvalidQueueTransitionException("Cannot mark no-show for queue entry in status: " + entry.getStatus() + ". Entry must be CALLED.");
        }

        entry.setStatus(QueueStatus.NO_SHOW);
        entry.setCompletedAt(LocalDateTime.now());
        QueueEntry saved = queueEntryRepository.save(entry);
        return QueueEntryResponse.fromEntity(saved, 0, 0);
    }

    @Transactional
    public QueueEntryResponse leaveQueue(Long entryId, CustomUserDetails currentUser) {
        QueueEntry entry = queueEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found with id: " + entryId));

        if (currentUser == null || !entry.getCustomer().getId().equals(currentUser.getId())) {
            throw new UnauthorizedResourceAccessException("You are not authorized to remove this customer from the queue");
        }

        if (entry.getStatus() != QueueStatus.WAITING) {
            throw new InvalidQueueTransitionException("Cannot leave queue from status: " + entry.getStatus() + ". Only WAITING entries can leave.");
        }

        entry.setStatus(QueueStatus.LEFT);
        QueueEntry saved = queueEntryRepository.save(entry);
        return QueueEntryResponse.fromEntity(saved, 0, 0);
    }

    @Transactional(readOnly = true)
    public Page<QueueEntryResponse> getQueueEntries(Long providerId, LocalDate date, QueueStatus status, Pageable pageable, CustomUserDetails currentUser) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + providerId));

        providerService.checkOwnershipOrAdmin(provider, currentUser);

        LocalDate queueDate = date != null ? date : LocalDate.now();
        Page<QueueEntry> entries;

        if (status != null) {
            entries = queueEntryRepository.findByProviderIdAndQueueDateAndStatus(providerId, queueDate, status, pageable);
        } else {
            entries = queueEntryRepository.findByProviderIdAndQueueDate(providerId, queueDate, pageable);
        }

        return entries.map(e -> QueueEntryResponse.fromEntity(e, 0, 0));
    }
}
