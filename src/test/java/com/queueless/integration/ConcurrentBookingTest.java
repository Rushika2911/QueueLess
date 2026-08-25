package com.queueless.integration;

import com.queueless.dto.appointment.CreateAppointmentRequest;
import com.queueless.entity.Provider;
import com.queueless.entity.Service;
import com.queueless.entity.User;
import com.queueless.entity.WorkingHour;
import com.queueless.entity.enums.ProviderStatus;
import com.queueless.entity.enums.Role;
import com.queueless.repository.AppointmentRepository;
import com.queueless.repository.ProviderRepository;
import com.queueless.repository.ServiceRepository;
import com.queueless.repository.UserRepository;
import com.queueless.repository.WorkingHourRepository;
import com.queueless.security.CustomUserDetails;
import com.queueless.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ConcurrentBookingTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private WorkingHourRepository workingHourRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    private Provider provider;
    private Service service;
    private LocalDate targetDate;
    private List<User> customers;

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
        workingHourRepository.deleteAll();
        serviceRepository.deleteAll();
        providerRepository.deleteAll();

        String suffix = UUID.randomUUID().toString().substring(0, 6);
        User providerUser = userRepository.save(new User("Dr. Concurrent", "doc.concurrent." + suffix + "@example.com", "pass", Role.PROVIDER, true));
        provider = providerRepository.save(new Provider(providerUser, "Surgery", "Desc", ProviderStatus.ACTIVE));
        service = serviceRepository.save(new Service(provider, "Checkup", "Desc", 30, new BigDecimal("100.00"), true));

        targetDate = LocalDate.now().plusDays(10);
        workingHourRepository.save(new WorkingHour(provider, targetDate.getDayOfWeek(), LocalTime.of(8, 0), LocalTime.of(18, 0)));

        customers = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            customers.add(userRepository.save(new User("Customer " + i, "c" + i + "." + suffix + "@example.com", "pass", Role.CUSTOMER, true)));
        }
    }

    @Test
    @DisplayName("Concurrent booking test: Only 1 thread should succeed for the same slot")
    void testConcurrentBookingSingleSuccess() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final User customer = customers.get(i);
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    CreateAppointmentRequest req = new CreateAppointmentRequest(provider.getId(), service.getId(), targetDate, LocalTime.of(10, 0));
                    appointmentService.bookAppointment(req, new CustomUserDetails(customer));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            }));
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(threadCount - 1);
        assertThat(appointmentRepository.count()).isEqualTo(1);
    }
}
