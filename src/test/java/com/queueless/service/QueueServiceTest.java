package com.queueless.service;

import com.queueless.dto.queue.JoinQueueRequest;
import com.queueless.dto.queue.QueueEntryResponse;
import com.queueless.entity.Provider;
import com.queueless.entity.QueueEntry;
import com.queueless.entity.User;
import com.queueless.entity.enums.ProviderStatus;
import com.queueless.entity.enums.QueueStatus;
import com.queueless.entity.enums.Role;
import com.queueless.exception.InvalidQueueTransitionException;
import com.queueless.exception.QueueConflictException;
import com.queueless.repository.AppointmentRepository;
import com.queueless.repository.ProviderRepository;
import com.queueless.repository.QueueEntryRepository;
import com.queueless.repository.UserRepository;
import com.queueless.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    @Mock
    private QueueEntryRepository queueEntryRepository;

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ProviderService providerService;

    @InjectMocks
    private QueueService queueService;

    private User customerUser;
    private CustomUserDetails customerDetails;
    private User providerUser;
    private CustomUserDetails providerDetails;
    private Provider provider;

    @BeforeEach
    void setUp() {
        customerUser = new User("Customer", "c@example.com", "pass", Role.CUSTOMER, true);
        customerUser.setId(1L);
        customerDetails = new CustomUserDetails(customerUser);

        providerUser = new User("Doc", "doc@example.com", "pass", Role.PROVIDER, true);
        providerUser.setId(2L);
        providerDetails = new CustomUserDetails(providerUser);

        provider = new Provider(providerUser, "Dentist", "Desc", ProviderStatus.ACTIVE);
        provider.setId(10L);
    }

    @Test
    @DisplayName("Should successfully join queue and generate token")
    void shouldJoinQueueSuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(customerUser));
        when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));
        when(queueEntryRepository.findByCustomerIdAndProviderIdAndQueueDateAndStatusIn(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(queueEntryRepository.findTopByProviderIdAndQueueDateOrderByTokenNumberDesc(any(), any())).thenReturn(Optional.empty());

        QueueEntry saved = new QueueEntry(provider, customerUser, null, LocalDate.now(), 1, 1, QueueStatus.WAITING);
        saved.setId(100L);
        when(queueEntryRepository.save(any(QueueEntry.class))).thenReturn(saved);

        QueueEntryResponse response = queueService.joinQueue(10L, new JoinQueueRequest(), customerDetails);

        assertThat(response).isNotNull();
        assertThat(response.getTokenNumber()).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo(QueueStatus.WAITING);
    }

    @Test
    @DisplayName("Should throw QueueConflictException if customer already has active entry today")
    void shouldThrowConflictWhenCustomerAlreadyActive() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(customerUser));
        when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));

        QueueEntry active = new QueueEntry(provider, customerUser, null, LocalDate.now(), 1, 1, QueueStatus.WAITING);
        when(queueEntryRepository.findByCustomerIdAndProviderIdAndQueueDateAndStatusIn(any(), any(), any(), any())).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> queueService.joinQueue(10L, new JoinQueueRequest(), customerDetails))
                .isInstanceOf(QueueConflictException.class)
                .hasMessageContaining("already has an active queue entry");
    }

    @Test
    @DisplayName("Should transition CALLED entry to SERVING")
    void shouldStartServingCalledEntry() {
        QueueEntry entry = new QueueEntry(provider, customerUser, null, LocalDate.now(), 1, 1, QueueStatus.CALLED);
        entry.setId(100L);

        when(queueEntryRepository.findById(100L)).thenReturn(Optional.of(entry));
        when(queueEntryRepository.findByProviderIdAndQueueDateAndStatus(10L, LocalDate.now(), QueueStatus.SERVING)).thenReturn(List.of());
        when(queueEntryRepository.save(any(QueueEntry.class))).thenReturn(entry);

        QueueEntryResponse response = queueService.startServing(100L, providerDetails);

        assertThat(response.getStatus()).isEqualTo(QueueStatus.SERVING);
    }

    @Test
    @DisplayName("Should throw InvalidQueueTransitionException when starting serving an uncalled entry")
    void shouldThrowInvalidTransitionWhenServingWaitingEntry() {
        QueueEntry entry = new QueueEntry(provider, customerUser, null, LocalDate.now(), 1, 1, QueueStatus.WAITING);
        entry.setId(100L);

        when(queueEntryRepository.findById(100L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> queueService.startServing(100L, providerDetails))
                .isInstanceOf(InvalidQueueTransitionException.class)
                .hasMessageContaining("Entry must be CALLED");
    }
}
