package com.queueless.dto.queue;

public class JoinQueueRequest {

    private Long appointmentId;

    public JoinQueueRequest() {
    }

    public JoinQueueRequest(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }
}
