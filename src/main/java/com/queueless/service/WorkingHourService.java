package com.queueless.service;

import com.queueless.dto.provider.WorkingHourRequest;
import com.queueless.dto.provider.WorkingHourResponse;
import com.queueless.entity.Provider;
import com.queueless.entity.WorkingHour;
import com.queueless.exception.ResourceNotFoundException;
import com.queueless.repository.ProviderRepository;
import com.queueless.repository.WorkingHourRepository;
import com.queueless.security.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WorkingHourService {

    private final WorkingHourRepository workingHourRepository;
    private final ProviderRepository providerRepository;
    private final ProviderService providerService;

    public WorkingHourService(
            WorkingHourRepository workingHourRepository,
            ProviderRepository providerRepository,
            ProviderService providerService
    ) {
        this.workingHourRepository = workingHourRepository;
        this.providerRepository = providerRepository;
        this.providerService = providerService;
    }

    @Transactional(readOnly = true)
    public List<WorkingHourResponse> getWorkingHoursByProvider(Long providerId) {
        if (!providerRepository.existsById(providerId)) {
            throw new ResourceNotFoundException("Provider not found with id: " + providerId);
        }

        return workingHourRepository.findByProviderId(providerId).stream()
                .map(WorkingHourResponse::fromEntity)
                .toList();
    }

    @Transactional
    public List<WorkingHourResponse> updateWorkingHours(Long providerId, List<WorkingHourRequest> requests, CustomUserDetails currentUser) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + providerId));

        providerService.checkOwnershipOrAdmin(provider, currentUser);

        // Validate individual requests and check overlapping intervals
        validateWorkingHours(requests);

        workingHourRepository.deleteByProviderId(providerId);

        List<WorkingHour> newWorkingHours = new ArrayList<>();
        for (WorkingHourRequest req : requests) {
            WorkingHour wh = new WorkingHour(
                    provider,
                    req.getDayOfWeek(),
                    req.getStartTime(),
                    req.getEndTime()
            );
            newWorkingHours.add(wh);
        }

        List<WorkingHour> saved = workingHourRepository.saveAll(newWorkingHours);

        return saved.stream()
                .map(WorkingHourResponse::fromEntity)
                .toList();
    }

    private void validateWorkingHours(List<WorkingHourRequest> requests) {
        if (requests == null) return;

        Map<DayOfWeek, List<WorkingHourRequest>> groupedByDay = requests.stream()
                .collect(Collectors.groupingBy(WorkingHourRequest::getDayOfWeek));

        for (Map.Entry<DayOfWeek, List<WorkingHourRequest>> entry : groupedByDay.entrySet()) {
            List<WorkingHourRequest> dayHours = entry.getValue();

            for (WorkingHourRequest req : dayHours) {
                if (!req.getStartTime().isBefore(req.getEndTime())) {
                    throw new IllegalArgumentException("Start time must be before end time for " + req.getDayOfWeek());
                }
            }

            // Check for overlaps within the same day
            for (int i = 0; i < dayHours.size(); i++) {
                for (int j = i + 1; j < dayHours.size(); j++) {
                    WorkingHourRequest h1 = dayHours.get(i);
                    WorkingHourRequest h2 = dayHours.get(j);

                    if (h1.getStartTime().isBefore(h2.getEndTime()) && h2.getStartTime().isBefore(h1.getEndTime())) {
                        throw new IllegalArgumentException("Overlapping working hours detected for " + entry.getKey());
                    }
                }
            }
        }
    }
}
