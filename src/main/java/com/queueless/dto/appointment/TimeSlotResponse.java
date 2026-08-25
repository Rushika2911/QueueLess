package com.queueless.dto.appointment;

import java.time.LocalTime;

public class TimeSlotResponse {

    private LocalTime startTime;
    private LocalTime endTime;
    private boolean available;

    public TimeSlotResponse() {
    }

    public TimeSlotResponse(LocalTime startTime, LocalTime endTime, boolean available) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.available = available;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
