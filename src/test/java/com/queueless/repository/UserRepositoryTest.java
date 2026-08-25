package com.queueless.repository;

import com.queueless.entity.User;
import com.queueless.entity.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should save and find user by email")
    void shouldSaveAndFindUserByEmail() {
        User user = new User("John Doe", "john@example.com", "hashedPassword", Role.CUSTOMER, true);
        entityManager.persistAndFlush(user);

        Optional<User> found = userRepository.findByEmail("john@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John Doe");
        assertThat(found.get().getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should check if user exists by email")
    void shouldCheckIfExistsByEmail() {
        User user = new User("Jane Doe", "jane@example.com", "hashedPassword", Role.PROVIDER, true);
        entityManager.persistAndFlush(user);

        boolean exists = userRepository.existsByEmail("jane@example.com");
        boolean notExists = userRepository.existsByEmail("nonexistent@example.com");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}
