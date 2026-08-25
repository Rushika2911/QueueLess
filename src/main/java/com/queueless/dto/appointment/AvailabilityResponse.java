package com.queueless.dto.appointment;

import java.time.LocalDate;
import java.util.List;

public class AvailabilityResponse {

    private LocalDate date;
    private Long serviceId;
    private List<TimeSlotResponse> slots;

    public AvailabilityResponse() {
    }

    public AvailabilityResponse(LocalDate date, Long serviceId, List<TimeSlotResponse> slots) {
        this.date = date;
        this.serviceId = serviceId;
        this.slots = slots;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public List<TimeSlotResponse> getSlots() {
        return slots;
    }

    public void setSlots(List<TimeSlotResponse> slots) {
        this.slots = slots;
    }
}
