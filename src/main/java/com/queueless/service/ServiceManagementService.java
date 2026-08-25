package com.queueless.service;

import com.queueless.dto.service.ServiceRequest;
import com.queueless.dto.service.ServiceResponse;
import com.queueless.entity.Provider;
import com.queueless.entity.Service;
import com.queueless.exception.ResourceNotFoundException;
import com.queueless.repository.ProviderRepository;
import com.queueless.repository.ServiceRepository;
import com.queueless.security.CustomUserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@org.springframework.stereotype.Service
public class ServiceManagementService {

    private final ServiceRepository serviceRepository;
    private final ProviderRepository providerRepository;
    private final ProviderService providerService;

    public ServiceManagementService(
            ServiceRepository serviceRepository,
            ProviderRepository providerRepository,
            ProviderService providerService
    ) {
        this.serviceRepository = serviceRepository;
        this.providerRepository = providerRepository;
        this.providerService = providerService;
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> getServicesByProvider(Long providerId, boolean activeOnly) {
        if (!providerRepository.existsById(providerId)) {
            throw new ResourceNotFoundException("Provider not found with id: " + providerId);
        }

        List<Service> services = activeOnly
                ? serviceRepository.findByProviderIdAndActiveTrue(providerId)
                : serviceRepository.findByProviderId(providerId);

        return services.stream()
                .map(ServiceResponse::fromEntity)
                .toList();
    }

    @Transactional
    public ServiceResponse createService(Long providerId, ServiceRequest request, CustomUserDetails currentUser) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + providerId));

        providerService.checkOwnershipOrAdmin(provider, currentUser);

        Service service = new Service(
                provider,
                request.getName(),
                request.getDescription(),
                request.getDurationMinutes(),
                request.getPrice(),
                request.getActive() != null ? request.getActive() : true
        );

        Service saved = serviceRepository.save(service);
        return ServiceResponse.fromEntity(saved);
    }

    @Transactional
    public ServiceResponse updateService(Long serviceId, ServiceRequest request, CustomUserDetails currentUser) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));

        providerService.checkOwnershipOrAdmin(service.getProvider(), currentUser);

        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setDurationMinutes(request.getDurationMinutes());
        service.setPrice(request.getPrice());
        if (request.getActive() != null) {
            service.setActive(request.getActive());
        }

        Service updated = serviceRepository.save(service);
        return ServiceResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteService(Long serviceId, CustomUserDetails currentUser) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));

        providerService.checkOwnershipOrAdmin(service.getProvider(), currentUser);

        // Soft deletion as specified in section 8 of project_spec.md
        service.setActive(false);
        serviceRepository.save(service);
    }
}
