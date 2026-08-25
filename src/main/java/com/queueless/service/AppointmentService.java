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
import com.queueless.exception.ResourceNotFoundException;
import com.queueless.exception.SlotUnavailableException;
import com.queueless.exception.UnauthorizedResourceAccessException;
import com.queueless.repository.*;
import com.queueless.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@org.springframework.stereotype.Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ProviderRepository providerRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final WorkingHourRepository workingHourRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            ProviderRepository providerRepository,
            ServiceRepository serviceRepository,
            UserRepository userRepository,
            WorkingHourRepository workingHourRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.providerRepository = providerRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.workingHourRepository = workingHourRepository;
    }

    @Transactional
    public AppointmentResponse bookAppointment(CreateAppointmentRequest request, CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedResourceAccessException("Authentication required to book an appointment");
        }

        User customer = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Provider provider = providerRepository.findById(request.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + request.getProviderId()));

        if (provider.getStatus() != ProviderStatus.ACTIVE) {
            throw new InvalidAppointmentException("Provider is currently inactive and cannot accept appointments");
        }

        Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + request.getServiceId()));

        if (!service.getProvider().getId().equals(provider.getId()) || Boolean.FALSE.equals(service.getActive())) {
            throw new InvalidAppointmentException("Service is inactive or does not belong to the selected provider");
        }

        LocalDate date = request.getAppointmentDate();
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = startTime.plusMinutes(service.getDurationMinutes());
        LocalDateTime startDateTime = LocalDateTime.of(date, startTime);

        if (startDateTime.isBefore(LocalDateTime.now())) {
            throw new InvalidAppointmentException("Cannot book an appointment in the past");
        }

        // Validate working hours
        List<WorkingHour> workingHours = workingHourRepository.findByProviderIdAndDayOfWeek(provider.getId(), date.getDayOfWeek());
        boolean withinWorkingHours = false;

        for (WorkingHour wh : workingHours) {
            if (!startTime.isBefore(wh.getStartTime()) && !endTime.isAfter(wh.getEndTime())) {
                withinWorkingHours = true;
                break;
            }
        }

        if (!withinWorkingHours) {
            throw new InvalidAppointmentException("Requested appointment time falls outside provider working hours");
        }

        // Concurrency double booking check for Provider
        List<Appointment> providerOverlaps = appointmentRepository.findOverlappingAppointmentsForProvider(
                provider.getId(), date, startTime, endTime
        );
        if (!providerOverlaps.isEmpty()) {
            throw new SlotUnavailableException("The requested appointment slot is no longer available");
        }

        // Double booking check for Customer
        List<Appointment> customerOverlaps = appointmentRepository.findOverlappingAppointmentsForCustomer(
                customer.getId(), date, startTime, endTime
        );
        if (!customerOverlaps.isEmpty()) {
            throw new SlotUnavailableException("Customer already has an appointment during this time interval");
        }

        Appointment appointment = new Appointment(
                customer,
                provider,
                service,
                date,
                startTime,
                endTime,
                AppointmentStatus.BOOKED
        );

        Appointment saved = appointmentRepository.save(appointment);
        return AppointmentResponse.fromEntity(saved);
    }

    @Transactional
    public AppointmentResponse cancelAppointment(Long appointmentId, CustomUserDetails currentUser) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        checkAppointmentAccessOrOwner(appointment, currentUser);

        if (appointment.getStatus() == AppointmentStatus.CANCELLED ||
            appointment.getStatus() == AppointmentStatus.COMPLETED ||
            appointment.getStatus() == AppointmentStatus.NO_SHOW) {
            throw new InvalidAppointmentException("Appointment cannot be cancelled in its current status: " + appointment.getStatus());
        }

        // 30-minute rule cancellation enforcement
        LocalDateTime startDateTime = LocalDateTime.of(appointment.getAppointmentDate(), appointment.getStartTime());
        if (LocalDateTime.now().isAfter(startDateTime.minusMinutes(30))) {
            throw new InvalidAppointmentException("Cancellation is allowed only up to 30 minutes before appointment start time");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment updated = appointmentRepository.save(appointment);
        return AppointmentResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointments(LocalDate date, AppointmentStatus status, Pageable pageable, CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedResourceAccessException("Authentication required");
        }

        User user = currentUser.getUser();
        Page<Appointment> appointments;

        if (user.getRole() == Role.ADMIN) {
            if (date != null && status != null) {
                appointments = appointmentRepository.findByProviderIdAndStatusAndAppointmentDate(null, status, date, pageable);
            } else {
                appointments = appointmentRepository.findAll(pageable);
            }
        } else if (user.getRole() == Role.PROVIDER) {
            Provider provider = providerRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found for user"));
            if (date != null && status != null) {
                appointments = appointmentRepository.findByProviderIdAndStatusAndAppointmentDate(provider.getId(), status, date, pageable);
            } else if (date != null) {
                appointments = appointmentRepository.findByProviderIdAndAppointmentDate(provider.getId(), date, pageable);
            } else if (status != null) {
                appointments = appointmentRepository.findByProviderIdAndStatus(provider.getId(), status, pageable);
            } else {
                appointments = appointmentRepository.findByProviderId(provider.getId(), pageable);
            }
        } else {
            if (date != null && status != null) {
                appointments = appointmentRepository.findByCustomerIdAndStatusAndAppointmentDate(user.getId(), status, date, pageable);
            } else if (date != null) {
                appointments = appointmentRepository.findByCustomerIdAndAppointmentDate(user.getId(), date, pageable);
            } else if (status != null) {
                appointments = appointmentRepository.findByCustomerIdAndStatus(user.getId(), status, pageable);
            } else {
                appointments = appointmentRepository.findByCustomerId(user.getId(), pageable);
            }
        }

        return appointments.map(AppointmentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long id, CustomUserDetails currentUser) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        checkAppointmentAccessOrOwner(appointment, currentUser);

        return AppointmentResponse.fromEntity(appointment);
    }

    private void checkAppointmentAccessOrOwner(Appointment appointment, CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedResourceAccessException("Authentication required");
        }

        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean isCustomerOwner = appointment.getCustomer().getId().equals(currentUser.getId());
        boolean isProviderOwner = appointment.getProvider().getUser() != null &&
                                  appointment.getProvider().getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isCustomerOwner && !isProviderOwner) {
            throw new UnauthorizedResourceAccessException("You are not authorized to access or modify this appointment");
        }
    }
}
