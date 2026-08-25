package com.queueless.service;

import com.queueless.dto.appointment.AppointmentResponse;
import com.queueless.dto.appointment.CreateAppointmentRequest;
import com.queueless.entity.Appointment;
import com.queueless.entity.Provider;
import com.queueless.entity.Service;
import com.queueless.entity.User;
import com.queueless.entity.WorkingHour;
import com.queueless.entity.enums.AppointmentStatus;
import com.queueless.entity.enums.ProviderStatus;
import com.queueless.entity.enums.Role;
import com.queueless.exception.InvalidAppointmentException;
import com.queueless.exception.SlotUnavailableException;
import com.queueless.repository.AppointmentRepository;
import com.queueless.repository.ProviderRepository;
import com.queueless.repository.ServiceRepository;
import com.queueless.repository.UserRepository;
import com.queueless.repository.WorkingHourRepository;
import com.queueless.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkingHourRepository workingHourRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private User customerUser;
    private CustomUserDetails customerDetails;
    private Provider provider;
    private Service service;
    private LocalDate futureDate;

    @BeforeEach
    void setUp() {
        customerUser = new User("Rushika", "rushika@example.com", "pass", Role.CUSTOMER, true);
        customerUser.setId(1L);
        customerDetails = new CustomUserDetails(customerUser);

        User providerUser = new User("Doc", "doc@example.com", "pass", Role.PROVIDER, true);
        providerUser.setId(2L);

        provider = new Provider(providerUser, "Dentist", "Desc", ProviderStatus.ACTIVE);
        provider.setId(10L);

        service = new Service(provider, "Cleaning", "Desc", 30, new BigDecimal("60.00"), true);
        service.setId(100L);

        futureDate = LocalDate.now().plusDays(2);
    }

    @Test
    @DisplayName("Should successfully book appointment when slot is available within working hours")
    void shouldBookAppointmentSuccessfully() {
        CreateAppointmentRequest request = new CreateAppointmentRequest(10L, 100L, futureDate, LocalTime.of(10, 0));
        WorkingHour wh = new WorkingHour(provider, futureDate.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(17, 0));

        when(userRepository.findById(1L)).thenReturn(Optional.of(customerUser));
        when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));
        when(serviceRepository.findById(100L)).thenReturn(Optional.of(service));
        when(workingHourRepository.findByProviderIdAndDayOfWeek(10L, futureDate.getDayOfWeek())).thenReturn(List.of(wh));
        when(appointmentRepository.findOverlappingAppointmentsForProvider(10L, futureDate, LocalTime.of(10, 0), LocalTime.of(10, 30))).thenReturn(List.of());
        when(appointmentRepository.findOverlappingAppointmentsForCustomer(1L, futureDate, LocalTime.of(10, 0), LocalTime.of(10, 30))).thenReturn(List.of());

        Appointment savedApt = new Appointment(customerUser, provider, service, futureDate, LocalTime.of(10, 0), LocalTime.of(10, 30), AppointmentStatus.BOOKED);
        savedApt.setId(500L);

        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedApt);

        AppointmentResponse response = appointmentService.bookAppointment(request, customerDetails);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(500L);
        assertThat(response.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
    }

    @Test
    @DisplayName("Should throw SlotUnavailableException when provider slot overlaps with existing booking")
    void shouldThrowSlotUnavailableExceptionWhenOverlaps() {
        CreateAppointmentRequest request = new CreateAppointmentRequest(10L, 100L, futureDate, LocalTime.of(10, 0));
        WorkingHour wh = new WorkingHour(provider, futureDate.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(17, 0));

        when(userRepository.findById(1L)).thenReturn(Optional.of(customerUser));
        when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));
        when(serviceRepository.findById(100L)).thenReturn(Optional.of(service));
        when(workingHourRepository.findByProviderIdAndDayOfWeek(10L, futureDate.getDayOfWeek())).thenReturn(List.of(wh));

        Appointment existing = new Appointment(customerUser, provider, service, futureDate, LocalTime.of(10, 0), LocalTime.of(10, 30), AppointmentStatus.BOOKED);
        when(appointmentRepository.findOverlappingAppointmentsForProvider(10L, futureDate, LocalTime.of(10, 0), LocalTime.of(10, 30))).thenReturn(List.of(existing));

        assertThatThrownBy(() -> appointmentService.bookAppointment(request, customerDetails))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    @DisplayName("Should throw InvalidAppointmentException when cancelling late (within 30 minutes of start)")
    void shouldThrowExceptionWhenCancellingLate() {
        Appointment apt = new Appointment(customerUser, provider, service, LocalDate.now(), LocalTime.now().plusMinutes(10), LocalTime.now().plusMinutes(40), AppointmentStatus.BOOKED);
        apt.setId(500L);

        when(appointmentRepository.findById(500L)).thenReturn(Optional.of(apt));

        assertThatThrownBy(() -> appointmentService.cancelAppointment(500L, customerDetails))
                .isInstanceOf(InvalidAppointmentException.class)
                .hasMessageContaining("allowed only up to 30 minutes before");
    }
}
