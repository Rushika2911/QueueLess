package com.queueless.dto.queue;

import com.queueless.entity.enums.QueueStatus;

public class QueuePositionResponse {

    private Long queueEntryId;
    private Long providerId;
    private String providerName;
    private Integer tokenNumber;
    private Integer position;
    private Integer peopleAhead;
    private Integer estimatedWaitMinutes;
    private QueueStatus status;

    public QueuePositionResponse() {
    }

    public QueuePositionResponse(Long queueEntryId, Long providerId, String providerName, Integer tokenNumber, Integer position, Integer peopleAhead, Integer estimatedWaitMinutes, QueueStatus status) {
        this.queueEntryId = queueEntryId;
        this.providerId = providerId;
        this.providerName = providerName;
        this.tokenNumber = tokenNumber;
        this.position = position;
        this.peopleAhead = peopleAhead;
        this.estimatedWaitMinutes = estimatedWaitMinutes;
        this.status = status;
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

    public Integer getPeopleAhead() {
        return peopleAhead;
    }

    public void setPeopleAhead(Integer peopleAhead) {
        this.peopleAhead = peopleAhead;
    }

    public Integer getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }

    public void setEstimatedWaitMinutes(Integer estimatedWaitMinutes) {
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }

    public QueueStatus getStatus() {
        return status;
    }

    public void setStatus(QueueStatus status) {
        this.status = status;
    }
}
