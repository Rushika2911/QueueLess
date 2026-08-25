package com.queueless.dto.provider;

import com.queueless.entity.WorkingHour;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class WorkingHourResponse {

    private Long id;
    private Long providerId;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;

    public WorkingHourResponse() {
    }

    public WorkingHourResponse(Long id, Long providerId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.id = id;
        this.providerId = providerId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static WorkingHourResponse fromEntity(WorkingHour wh) {
        return new WorkingHourResponse(
                wh.getId(),
                wh.getProvider() != null ? wh.getProvider().getId() : null,
                wh.getDayOfWeek(),
                wh.getStartTime(),
                wh.getEndTime()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
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
}
