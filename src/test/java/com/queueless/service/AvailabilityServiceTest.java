package com.queueless.service;

import com.queueless.dto.appointment.AvailabilityResponse;
import com.queueless.entity.Appointment;
import com.queueless.entity.Provider;
import com.queueless.entity.Service;
import com.queueless.entity.User;
import com.queueless.entity.WorkingHour;
import com.queueless.entity.enums.AppointmentStatus;
import com.queueless.entity.enums.ProviderStatus;
import com.queueless.entity.enums.Role;
import com.queueless.repository.AppointmentRepository;
import com.queueless.repository.ProviderRepository;
import com.queueless.repository.ServiceRepository;
import com.queueless.repository.WorkingHourRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private WorkingHourRepository workingHourRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    private Provider provider;
    private Service service;
    private LocalDate futureDate;

    @BeforeEach
    void setUp() {
        User user = new User("Doc", "doc@example.com", "pass", Role.PROVIDER, true);
        user.setId(10L);

        provider = new Provider(user, "General", "Desc", ProviderStatus.ACTIVE);
        provider.setId(1L);

        service = new Service(provider, "Consultation", "Desc", 30, new BigDecimal("50.00"), true);
        service.setId(4L);

        futureDate = LocalDate.now().plusDays(5);
    }

    @Test
    @DisplayName("Should generate available slots based on working hours and existing appointments")
    void shouldGenerateAvailableSlots() {
        WorkingHour wh = new WorkingHour(provider, futureDate.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(10, 0));

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(serviceRepository.findById(4L)).thenReturn(Optional.of(service));
        when(workingHourRepository.findByProviderIdAndDayOfWeek(1L, futureDate.getDayOfWeek())).thenReturn(List.of(wh));

        User customer = new User("C1", "c1@example.com", "pass", Role.CUSTOMER, true);
        Appointment existing = new Appointment(customer, provider, service, futureDate, LocalTime.of(9, 0), LocalTime.of(9, 30), AppointmentStatus.BOOKED);

        when(appointmentRepository.findByProviderIdAndAppointmentDate(1L, futureDate)).thenReturn(List.of(existing));

        AvailabilityResponse response = availabilityService.getAvailability(1L, 4L, futureDate);

        assertThat(response).isNotNull();
        assertThat(response.getSlots()).hasSize(2);
        assertThat(response.getSlots().get(0).isAvailable()).isFalse(); // Overlaps existing appointment
        assertThat(response.getSlots().get(1).isAvailable()).isTrue();  // 09:30 - 10:00 available
    }
}
