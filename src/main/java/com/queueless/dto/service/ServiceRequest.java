package com.queueless.dto.service;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class ServiceRequest {

    @NotBlank(message = "Service name is required")
    private String name;

    private String description;

    @NotNull(message = "Duration in minutes is required")
    @Positive(message = "Duration must be positive")
    private Integer durationMinutes;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", message = "Price must be non-negative")
    private BigDecimal price;

    private Boolean active = true;

    public ServiceRequest() {
    }

    public ServiceRequest(String name, String description, Integer durationMinutes, BigDecimal price, Boolean active) {
        this.name = name;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.price = price;
        this.active = active != null ? active : true;
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
        this.active = active != null ? active : true;
    }
}
