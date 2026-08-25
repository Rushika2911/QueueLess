package com.queueless.service;

import com.queueless.dto.provider.WorkingHourRequest;
import com.queueless.dto.provider.WorkingHourResponse;
import com.queueless.entity.Provider;
import com.queueless.entity.User;
import com.queueless.entity.enums.ProviderStatus;
import com.queueless.entity.enums.Role;
import com.queueless.repository.ProviderRepository;
import com.queueless.repository.WorkingHourRepository;
import com.queueless.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkingHourServiceTest {

    @Mock
    private WorkingHourRepository workingHourRepository;

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private ProviderService providerService;

    @InjectMocks
    private WorkingHourService workingHourService;

    private Provider provider;
    private CustomUserDetails ownerDetails;

    @BeforeEach
    void setUp() {
        User user = new User("Doc", "doc@example.com", "pass", Role.PROVIDER, true);
        user.setId(5L);
        provider = new Provider(user, "General", "Desc", ProviderStatus.ACTIVE);
        provider.setId(1L);
        ownerDetails = new CustomUserDetails(user);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when start time is not before end time")
    void shouldThrowExceptionWhenStartTimeAfterEndTime() {
        WorkingHourRequest invalidReq = new WorkingHourRequest(DayOfWeek.MONDAY, LocalTime.of(17, 0), LocalTime.of(9, 0));

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));

        assertThatThrownBy(() -> workingHourService.updateWorkingHours(1L, List.of(invalidReq), ownerDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start time must be before end time");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when overlapping intervals exist for same day")
    void shouldThrowExceptionOnOverlappingIntervals() {
        WorkingHourRequest slot1 = new WorkingHourRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0));
        WorkingHourRequest slot2 = new WorkingHourRequest(DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(17, 0));

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));

        assertThatThrownBy(() -> workingHourService.updateWorkingHours(1L, List.of(slot1, slot2), ownerDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Overlapping working hours detected");
    }
}
