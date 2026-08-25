package com.queueless.scheduler;

import com.queueless.entity.QueueEntry;
import com.queueless.entity.enums.QueueStatus;
import com.queueless.repository.QueueEntryRepository;
import com.queueless.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class NoShowScheduler {

    private static final Logger log = LoggerFactory.getLogger(NoShowScheduler.class);

    private final QueueEntryRepository queueEntryRepository;
    private final AuditService auditService;

    public NoShowScheduler(QueueEntryRepository queueEntryRepository, AuditService auditService) {
        this.queueEntryRepository = queueEntryRepository;
        this.auditService = auditService;
    }

    @Scheduled(fixedDelayString = "${queueless.scheduler.no-show-delay-ms:60000}")
    @Transactional
    public void processNoShows() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        List<QueueEntry> overdueCalledEntries = queueEntryRepository.findByStatusAndCalledAtBefore(QueueStatus.CALLED, cutoff);

        if (!overdueCalledEntries.isEmpty()) {
            log.info("Processing {} overdue CALLED queue entries for auto no-show transition", overdueCalledEntries.size());

            for (QueueEntry entry : overdueCalledEntries) {
                entry.setStatus(QueueStatus.NO_SHOW);
                entry.setCompletedAt(LocalDateTime.now());
                queueEntryRepository.save(entry);

                auditService.log(
                        entry.getCustomer(),
                        "AUTO_NO_SHOW",
                        "QueueEntry",
                        entry.getId(),
                        "Auto-marked NO_SHOW after 5 minutes of no response to call"
                );
            }
        }
    }
}
