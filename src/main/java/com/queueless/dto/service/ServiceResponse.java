package com.queueless.dto.service;

import com.queueless.entity.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ServiceResponse {

    private Long id;
    private Long providerId;
    private String name;
    private String description;
    private Integer durationMinutes;
    private BigDecimal price;
    private Boolean active;
    private LocalDateTime createdAt;

    public ServiceResponse() {
    }

    public ServiceResponse(Long id, Long providerId, String name, String description, Integer durationMinutes, BigDecimal price, Boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.providerId = providerId;
        this.name = name;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.price = price;
        this.active = active;
        this.createdAt = createdAt;
    }

    public static ServiceResponse fromEntity(Service service) {
        return new ServiceResponse(
                service.getId(),
                service.getProvider() != null ? service.getProvider().getId() : null,
                service.getName(),
                service.getDescription(),
                service.getDurationMinutes(),
                service.getPrice(),
                service.getActive(),
                service.getCreatedAt()
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
