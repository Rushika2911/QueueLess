package com.queueless.repository;

import com.queueless.entity.Provider;
import com.queueless.entity.QueueEntry;
import com.queueless.entity.User;
import com.queueless.entity.enums.ProviderStatus;
import com.queueless.entity.enums.QueueStatus;
import com.queueless.entity.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class QueueEntryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Test
    @DisplayName("Should find top token number for provider and date")
    void shouldFindTopTokenNumber() {
        User customer1 = entityManager.persistAndFlush(new User("C1", "c1@example.com", "pass", Role.CUSTOMER, true));
        User customer2 = entityManager.persistAndFlush(new User("C2", "c2@example.com", "pass", Role.CUSTOMER, true));

        User providerUser = entityManager.persistAndFlush(new User("Doc", "doc@example.com", "pass", Role.PROVIDER, true));
        Provider provider = entityManager.persistAndFlush(new Provider(providerUser, "General", "Desc", ProviderStatus.ACTIVE));

        LocalDate date = LocalDate.of(2026, 8, 25);

        entityManager.persistAndFlush(new QueueEntry(provider, customer1, null, date, 1, 1, QueueStatus.WAITING));
        entityManager.persistAndFlush(new QueueEntry(provider, customer2, null, date, 2, 2, QueueStatus.WAITING));

        Optional<QueueEntry> topEntry = queueEntryRepository.findTopByProviderIdAndQueueDateOrderByTokenNumberDesc(provider.getId(), date);

        assertThat(topEntry).isPresent();
        assertThat(topEntry.get().getTokenNumber()).isEqualTo(2);
    }
}
