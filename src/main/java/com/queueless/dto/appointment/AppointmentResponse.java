package com.queueless.dto.appointment;

import com.queueless.entity.Appointment;
import com.queueless.entity.enums.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AppointmentResponse {

    private Long id;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private Long providerId;
    private String providerName;
    private Long serviceId;
    private String serviceName;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private AppointmentStatus status;
    private LocalDateTime createdAt;

    public AppointmentResponse() {
    }

    public AppointmentResponse(Long id, Long customerId, String customerName, String customerEmail, Long providerId, String providerName, Long serviceId, String serviceName, LocalDate appointmentDate, LocalTime startTime, LocalTime endTime, AppointmentStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.providerId = providerId;
        this.providerName = providerName;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.appointmentDate = appointmentDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static AppointmentResponse fromEntity(Appointment apt) {
        return new AppointmentResponse(
                apt.getId(),
                apt.getCustomer() != null ? apt.getCustomer().getId() : null,
                apt.getCustomer() != null ? apt.getCustomer().getName() : null,
                apt.getCustomer() != null ? apt.getCustomer().getEmail() : null,
                apt.getProvider() != null ? apt.getProvider().getId() : null,
                apt.getProvider() != null && apt.getProvider().getUser() != null ? apt.getProvider().getUser().getName() : null,
                apt.getService() != null ? apt.getService().getId() : null,
                apt.getService() != null ? apt.getService().getName() : null,
                apt.getAppointmentDate(),
                apt.getStartTime(),
                apt.getEndTime(),
                apt.getStatus(),
                apt.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
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

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
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

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
