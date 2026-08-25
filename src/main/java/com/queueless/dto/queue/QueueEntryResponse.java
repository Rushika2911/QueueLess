package com.queueless.dto.queue;

import com.queueless.entity.QueueEntry;
import com.queueless.entity.enums.QueueStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class QueueEntryResponse {

    private Long queueEntryId;
    private Long providerId;
    private String providerName;
    private Long customerId;
    private String customerName;
    private Long appointmentId;
    private LocalDate queueDate;
    private Integer tokenNumber;
    private Integer position;
    private QueueStatus status;
    private Integer estimatedWaitMinutes;
    private LocalDateTime joinedAt;
    private LocalDateTime calledAt;
    private LocalDateTime completedAt;

    public QueueEntryResponse() {
    }

    public QueueEntryResponse(Long queueEntryId, Long providerId, String providerName, Long customerId, String customerName, Long appointmentId, LocalDate queueDate, Integer tokenNumber, Integer position, QueueStatus status, Integer estimatedWaitMinutes, LocalDateTime joinedAt, LocalDateTime calledAt, LocalDateTime completedAt) {
        this.queueEntryId = queueEntryId;
        this.providerId = providerId;
        this.providerName = providerName;
        this.customerId = customerId;
        this.customerName = customerName;
        this.appointmentId = appointmentId;
        this.queueDate = queueDate;
        this.tokenNumber = tokenNumber;
        this.position = position;
        this.status = status;
        this.estimatedWaitMinutes = estimatedWaitMinutes;
        this.joinedAt = joinedAt;
        this.calledAt = calledAt;
        this.completedAt = completedAt;
    }

    public static QueueEntryResponse fromEntity(QueueEntry entry, int calculatedPosition, int estimatedWaitMinutes) {
        return new QueueEntryResponse(
                entry.getId(),
                entry.getProvider() != null ? entry.getProvider().getId() : null,
                entry.getProvider() != null && entry.getProvider().getUser() != null ? entry.getProvider().getUser().getName() : null,
                entry.getCustomer() != null ? entry.getCustomer().getId() : null,
                entry.getCustomer() != null ? entry.getCustomer().getName() : null,
                entry.getAppointment() != null ? entry.getAppointment().getId() : null,
                entry.getQueueDate(),
                entry.getTokenNumber(),
                calculatedPosition,
                entry.getStatus(),
                estimatedWaitMinutes,
                entry.getJoinedAt(),
                entry.getCalledAt(),
                entry.getCompletedAt()
        );
    }

    public Long getQueueEntryId() {
        return queueEntryId;
    }

    public void setQueueEntryId(Long queueEntryId) {
        this.queueEntryId = queueEntryId;
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public LocalDate getQueueDate() {
        return queueDate;
    }

    public void setQueueDate(LocalDate queueDate) {
        this.queueDate = queueDate;
    }

    public Integer getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(Integer tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public QueueStatus getStatus() {
        return status;
    }

    public void setStatus(QueueStatus status) {
        this.status = status;
    }

    public Integer getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }

    public void setEstimatedWaitMinutes(Integer estimatedWaitMinutes) {
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public LocalDateTime getCalledAt() {
        return calledAt;
    }

    public void setCalledAt(LocalDateTime calledAt) {
        this.calledAt = calledAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
