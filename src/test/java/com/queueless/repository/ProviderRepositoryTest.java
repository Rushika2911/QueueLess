package com.queueless.repository;

import com.queueless.entity.Provider;
import com.queueless.entity.User;
import com.queueless.entity.enums.ProviderStatus;
import com.queueless.entity.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProviderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProviderRepository providerRepository;

    @Test
    @DisplayName("Should find provider by user id")
    void shouldFindProviderByUserId() {
        User user = new User("Dr. Smith", "smith@example.com", "pass", Role.PROVIDER, true);
        user = entityManager.persistAndFlush(user);

        Provider provider = new Provider(user, "Dentist", "Dental Clinic", ProviderStatus.ACTIVE);
        entityManager.persistAndFlush(provider);

        Optional<Provider> found = providerRepository.findByUserId(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getSpecialization()).isEqualTo("Dentist");
    }

    @Test
    @DisplayName("Should find providers by status and pagination")
    void shouldFindProvidersByStatus() {
        User u1 = entityManager.persistAndFlush(new User("Doc 1", "doc1@example.com", "pass", Role.PROVIDER, true));
        User u2 = entityManager.persistAndFlush(new User("Doc 2", "doc2@example.com", "pass", Role.PROVIDER, true));

        entityManager.persistAndFlush(new Provider(u1, "Cardiology", "Heart", ProviderStatus.ACTIVE));
        entityManager.persistAndFlush(new Provider(u2, "Neurology", "Brain", ProviderStatus.INACTIVE));

        Page<Provider> activePage = providerRepository.findByStatus(ProviderStatus.ACTIVE, PageRequest.of(0, 10));

        assertThat(activePage.getContent()).hasSize(1);
        assertThat(activePage.getContent().get(0).getSpecialization()).isEqualTo("Cardiology");
    }
}
