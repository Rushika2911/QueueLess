package com.queueless.dto.provider;

public class UpdateProviderRequest {

    private String specialization;
    private String description;

    public UpdateProviderRequest() {
    }

    public UpdateProviderRequest(String specialization, String description) {
        this.specialization = specialization;
        this.description = description;
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
}
