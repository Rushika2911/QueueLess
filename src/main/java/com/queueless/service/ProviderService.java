package com.queueless.service;

import com.queueless.dto.provider.CreateProviderRequest;
import com.queueless.dto.provider.ProviderResponse;
import com.queueless.dto.provider.UpdateProviderRequest;
import com.queueless.entity.Provider;
import com.queueless.entity.User;
import com.queueless.entity.enums.ProviderStatus;
import com.queueless.exception.DuplicateResourceException;
import com.queueless.exception.ResourceNotFoundException;
import com.queueless.exception.UnauthorizedResourceAccessException;
import com.queueless.repository.ProviderRepository;
import com.queueless.repository.UserRepository;
import com.queueless.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderService {

    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;

    public ProviderService(ProviderRepository providerRepository, UserRepository userRepository) {
        this.providerRepository = providerRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProviderResponse> getAllProviders(ProviderStatus status, String specialization, Pageable pageable) {
        Page<Provider> providers;

        if (status != null && specialization != null && !specialization.isBlank()) {
            providers = providerRepository.findByStatusAndSpecializationContainingIgnoreCase(status, specialization, pageable);
        } else if (status != null) {
            providers = providerRepository.findByStatus(status, pageable);
        } else if (specialization != null && !specialization.isBlank()) {
            providers = providerRepository.findBySpecializationContainingIgnoreCase(specialization, pageable);
        } else {
            providers = providerRepository.findAll(pageable);
        }

        return providers.map(ProviderResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public ProviderResponse getProviderById(Long id) {
        Provider provider = providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + id));
        return ProviderResponse.fromEntity(provider);
    }

    @Transactional
    public ProviderResponse createProvider(CreateProviderRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        if (providerRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new DuplicateResourceException("Provider record already exists for user id: " + request.getUserId());
        }

        Provider provider = new Provider(
                user,
                request.getSpecialization(),
                request.getDescription(),
                request.getStatus() != null ? request.getStatus() : ProviderStatus.ACTIVE
        );

        Provider saved = providerRepository.save(provider);
        return ProviderResponse.fromEntity(saved);
    }

    @Transactional
    public ProviderResponse updateProvider(Long id, UpdateProviderRequest request, CustomUserDetails currentUser) {
        Provider provider = providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + id));

        checkOwnershipOrAdmin(provider, currentUser);

        if (request.getSpecialization() != null) {
            provider.setSpecialization(request.getSpecialization());
        }
        if (request.getDescription() != null) {
            provider.setDescription(request.getDescription());
        }

        Provider updated = providerRepository.save(provider);
        return ProviderResponse.fromEntity(updated);
    }

    @Transactional
    public ProviderResponse updateProviderStatus(Long id, ProviderStatus status) {
        Provider provider = providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + id));

        provider.setStatus(status);
        Provider updated = providerRepository.save(provider);
        return ProviderResponse.fromEntity(updated);
    }

    public void checkOwnershipOrAdmin(Provider provider, CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedResourceAccessException("Authentication required");
        }
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = provider.getUser() != null && provider.getUser().getId().equals(currentUser.getId());
        if (!isAdmin && !isOwner) {
            throw new UnauthorizedResourceAccessException("You are not authorized to modify this provider resource");
        }
    }
}
