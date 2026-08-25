package com.queueless.scheduler;

import com.queueless.entity.Provider;
import com.queueless.entity.QueueEntry;
import com.queueless.entity.User;
import com.queueless.entity.enums.ProviderStatus;
import com.queueless.entity.enums.QueueStatus;
import com.queueless.entity.enums.Role;
import com.queueless.repository.QueueEntryRepository;
import com.queueless.service.AuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoShowSchedulerTest {

    @Mock
    private QueueEntryRepository queueEntryRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private NoShowScheduler noShowScheduler;

    @Test
    @DisplayName("Should auto-transition overdue CALLED entries to NO_SHOW")
    void shouldAutoTransitionOverdueEntries() {
        User customer = new User("Cust", "c@example.com", "pass", Role.CUSTOMER, true);
        User doc = new User("Doc", "d@example.com", "pass", Role.PROVIDER, true);
        Provider provider = new Provider(doc, "Dental", "Desc", ProviderStatus.ACTIVE);

        QueueEntry overdue = new QueueEntry(provider, customer, null, LocalDate.now(), 1, 1, QueueStatus.CALLED);
        overdue.setId(50L);
        overdue.setCalledAt(LocalDateTime.now().minusMinutes(6));

        when(queueEntryRepository.findByStatusAndCalledAtBefore(any(), any())).thenReturn(List.of(overdue));

        noShowScheduler.processNoShows();

        assertThat(overdue.getStatus()).isEqualTo(QueueStatus.NO_SHOW);
        verify(queueEntryRepository).save(overdue);
        verify(auditService).log(any(), any(), any(), any(), any());
    }
}
