package com.queueless.dto.provider;

import com.queueless.entity.Provider;
import com.queueless.entity.enums.ProviderStatus;

import java.time.LocalDateTime;

public class ProviderResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String specialization;
    private String description;
    private ProviderStatus status;
    private LocalDateTime createdAt;

    public ProviderResponse() {
    }

    public ProviderResponse(Long id, Long userId, String userName, String userEmail, String specialization, String description, ProviderStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.specialization = specialization;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static ProviderResponse fromEntity(Provider provider) {
        return new ProviderResponse(
                provider.getId(),
                provider.getUser() != null ? provider.getUser().getId() : null,
                provider.getUser() != null ? provider.getUser().getName() : null,
                provider.getUser() != null ? provider.getUser().getEmail() : null,
                provider.getSpecialization(),
                provider.getDescription(),
                provider.getStatus(),
                provider.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
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
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
