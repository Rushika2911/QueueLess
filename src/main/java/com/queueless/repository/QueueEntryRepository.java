package com.queueless.repository;

import com.queueless.entity.QueueEntry;
import com.queueless.entity.enums.QueueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    Optional<QueueEntry> findTopByProviderIdAndQueueDateOrderByTokenNumberDesc(Long providerId, LocalDate queueDate);

    List<QueueEntry> findByProviderIdAndQueueDateAndStatus(Long providerId, LocalDate queueDate, QueueStatus status);

    List<QueueEntry> findByProviderIdAndQueueDateAndStatusOrderByJoinedAtAsc(Long providerId, LocalDate queueDate, QueueStatus status);

    Optional<QueueEntry> findFirstByProviderIdAndQueueDateAndStatusOrderByJoinedAtAsc(Long providerId, LocalDate queueDate, QueueStatus status);

    Optional<QueueEntry> findByCustomerIdAndProviderIdAndQueueDateAndStatusIn(Long customerId, Long providerId, LocalDate queueDate, Collection<QueueStatus> statuses);

    Optional<QueueEntry> findByCustomerIdAndQueueDateAndStatusIn(Long customerId, LocalDate queueDate, Collection<QueueStatus> statuses);

    Page<QueueEntry> findByProviderIdAndQueueDate(Long providerId, LocalDate queueDate, Pageable pageable);

    Page<QueueEntry> findByProviderIdAndQueueDateAndStatus(Long providerId, LocalDate queueDate, QueueStatus status, Pageable pageable);

    long countByProviderIdAndQueueDateAndStatusAndJoinedAtBefore(Long providerId, LocalDate queueDate, QueueStatus status, LocalDateTime joinedAt);

    List<QueueEntry> findByStatusAndCalledAtBefore(QueueStatus status, LocalDateTime cutoff);
}
