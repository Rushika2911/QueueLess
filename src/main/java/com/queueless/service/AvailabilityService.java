package com.queueless.service;

import com.queueless.dto.appointment.AvailabilityResponse;
import com.queueless.dto.appointment.TimeSlotResponse;
import com.queueless.entity.Appointment;
import com.queueless.entity.Provider;
import com.queueless.entity.Service;
import com.queueless.entity.WorkingHour;
import com.queueless.entity.enums.ProviderStatus;
import com.queueless.exception.ResourceNotFoundException;
import com.queueless.repository.AppointmentRepository;
import com.queueless.repository.ProviderRepository;
import com.queueless.repository.ServiceRepository;
import com.queueless.repository.WorkingHourRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Service
public class AvailabilityService {

    private final ProviderRepository providerRepository;
    private final ServiceRepository serviceRepository;
    private final WorkingHourRepository workingHourRepository;
    private final AppointmentRepository appointmentRepository;

    public AvailabilityService(
            ProviderRepository providerRepository,
            ServiceRepository serviceRepository,
            WorkingHourRepository workingHourRepository,
            AppointmentRepository appointmentRepository
    ) {
        this.providerRepository = providerRepository;
        this.serviceRepository = serviceRepository;
        this.workingHourRepository = workingHourRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public AvailabilityResponse getAvailability(Long providerId, Long serviceId, LocalDate date) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + providerId));

        if (provider.getStatus() != ProviderStatus.ACTIVE) {
            return new AvailabilityResponse(date, serviceId, List.of());
        }

        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));

        if (!service.getProvider().getId().equals(providerId) || Boolean.FALSE.equals(service.getActive())) {
            return new AvailabilityResponse(date, serviceId, List.of());
        }

        List<WorkingHour> workingHours = workingHourRepository.findByProviderIdAndDayOfWeek(providerId, date.getDayOfWeek());
        if (workingHours.isEmpty()) {
            return new AvailabilityResponse(date, serviceId, List.of());
        }

        List<Appointment> existingAppointments = appointmentRepository.findByProviderIdAndAppointmentDate(providerId, date);
        int duration = service.getDurationMinutes();
        List<TimeSlotResponse> timeSlots = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();

        for (WorkingHour wh : workingHours) {
            LocalTime slotStart = wh.getStartTime();
            while (!slotStart.plusMinutes(duration).isAfter(wh.getEndTime())) {
                LocalTime slotEnd = slotStart.plusMinutes(duration);

                boolean isPast = date.isBefore(today) || (date.isEqual(today) && slotStart.isBefore(nowTime));
                boolean overlaps = false;

                if (!isPast) {
                    for (Appointment apt : existingAppointments) {
                        if (apt.getStatus() != com.queueless.entity.enums.AppointmentStatus.CANCELLED) {
                            if (slotStart.isBefore(apt.getEndTime()) && slotEnd.isAfter(apt.getStartTime())) {
                                overlaps = true;
                                break;
                            }
                        }
                    }
                }

                boolean available = !isPast && !overlaps;
                timeSlots.add(new TimeSlotResponse(slotStart, slotEnd, available));
                slotStart = slotEnd;
            }
        }

        return new AvailabilityResponse(date, serviceId, timeSlots);
    }
}
