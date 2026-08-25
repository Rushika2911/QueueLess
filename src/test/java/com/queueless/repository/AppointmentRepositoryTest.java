package com.queueless.repository;

import com.queueless.entity.Appointment;
import com.queueless.entity.Provider;
import com.queueless.entity.Service;
import com.queueless.entity.User;
import com.queueless.entity.enums.AppointmentStatus;
import com.queueless.entity.enums.ProviderStatus;
import com.queueless.entity.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AppointmentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    @DisplayName("Should detect overlapping appointments for provider")
    void shouldDetectOverlappingAppointmentsForProvider() {
        User customer = entityManager.persistAndFlush(new User("Customer 1", "c1@example.com", "pass", Role.CUSTOMER, true));
        User providerUser = entityManager.persistAndFlush(new User("Doc", "doc@example.com", "pass", Role.PROVIDER, true));
        Provider provider = entityManager.persistAndFlush(new Provider(providerUser, "General", "Desc", ProviderStatus.ACTIVE));
        Service service = entityManager.persistAndFlush(new Service(provider, "Consultation", "Desc", 30, new BigDecimal("50.00"), true));

        LocalDate date = LocalDate.of(2026, 8, 25);
        Appointment existing = new Appointment(customer, provider, service, date, LocalTime.of(10, 0), LocalTime.of(10, 30), AppointmentStatus.BOOKED);
        entityManager.persistAndFlush(existing);

        // Test overlap from 10:15 to 10:45
        List<Appointment> overlaps = appointmentRepository.findOverlappingAppointmentsForProvider(
                provider.getId(), date, LocalTime.of(10, 15), LocalTime.of(10, 45)
        );

        assertThat(overlaps).hasSize(1);
        assertThat(overlaps.get(0).getId()).isEqualTo(existing.getId());

        // Test non-overlapping slot from 10:30 to 11:00
        List<Appointment> nonOverlaps = appointmentRepository.findOverlappingAppointmentsForProvider(
                provider.getId(), date, LocalTime.of(10, 30), LocalTime.of(11, 0)
        );

        assertThat(nonOverlaps).isEmpty();
    }
}
