package com.queueless.service;

import com.queueless.dto.provider.CreateProviderRequest;
import com.queueless.dto.provider.ProviderResponse;
import com.queueless.dto.provider.UpdateProviderRequest;
import com.queueless.entity.Provider;
import com.queueless.entity.User;
import com.queueless.entity.enums.ProviderStatus;
import com.queueless.entity.enums.Role;
import com.queueless.exception.DuplicateResourceException;
import com.queueless.exception.ResourceNotFoundException;
import com.queueless.exception.UnauthorizedResourceAccessException;
import com.queueless.repository.ProviderRepository;
import com.queueless.repository.UserRepository;
import com.queueless.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProviderService providerService;

    private User providerUser;
    private Provider provider;
    private CustomUserDetails ownerDetails;
    private CustomUserDetails otherDetails;

    @BeforeEach
    void setUp() {
        providerUser = new User("Dr. Smith", "smith@example.com", "pass", Role.PROVIDER, true);
        providerUser.setId(10L);

        provider = new Provider(providerUser, "Dentistry", "Dental office", ProviderStatus.ACTIVE);
        provider.setId(1L);

        ownerDetails = new CustomUserDetails(providerUser);

        User otherUser = new User("Other User", "other@example.com", "pass", Role.PROVIDER, true);
        otherUser.setId(20L);
        otherDetails = new CustomUserDetails(otherUser);
    }

    @Test
    @DisplayName("Should create provider successfully for existing user")
    void shouldCreateProvider() {
        CreateProviderRequest request = new CreateProviderRequest(10L, "Dentistry", "Dental office", ProviderStatus.ACTIVE);

        when(userRepository.findById(10L)).thenReturn(Optional.of(providerUser));
        when(providerRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(providerRepository.save(any(Provider.class))).thenReturn(provider);

        ProviderResponse response = providerService.createProvider(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getSpecialization()).isEqualTo("Dentistry");
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when provider already exists for user")
    void shouldThrowDuplicateException() {
        CreateProviderRequest request = new CreateProviderRequest(10L, "Dentistry", "Dental office", ProviderStatus.ACTIVE);

        when(userRepository.findById(10L)).thenReturn(Optional.of(providerUser));
        when(providerRepository.findByUserId(10L)).thenReturn(Optional.of(provider));

        assertThatThrownBy(() -> providerService.createProvider(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Should update provider when requested by owner")
    void shouldUpdateProviderByOwner() {
        UpdateProviderRequest request = new UpdateProviderRequest("Orthodontics", "New desc");

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(providerRepository.save(any(Provider.class))).thenReturn(provider);

        ProviderResponse response = providerService.updateProvider(1L, request, ownerDetails);

        assertThat(response).isNotNull();
        verify(providerRepository, times(1)).save(provider);
    }

    @Test
    @DisplayName("Should throw UnauthorizedResourceAccessException when update requested by non-owner")
    void shouldThrowUnauthorizedWhenUpdatedByNonOwner() {
        UpdateProviderRequest request = new UpdateProviderRequest("Orthodontics", "New desc");

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));

        assertThatThrownBy(() -> providerService.updateProvider(1L, request, otherDetails))
                .isInstanceOf(UnauthorizedResourceAccessException.class);
    }
}
