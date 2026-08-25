package com.queueless.dto.provider;

import com.queueless.entity.enums.ProviderStatus;
import jakarta.validation.constraints.NotNull;

public class CreateProviderRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    private String specialization;
    private String description;
    private ProviderStatus status = ProviderStatus.ACTIVE;

    public CreateProviderRequest() {
    }

    public CreateProviderRequest(Long userId, String specialization, String description, ProviderStatus status) {
        this.userId = userId;
        this.specialization = specialization;
        this.description = description;
        this.status = status != null ? status : ProviderStatus.ACTIVE;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProviderStatus getStatus() {
        return status;
    }

    public void setStatus(ProviderStatus status) {
        this.status = status != null ? status : ProviderStatus.ACTIVE;
    }
}
