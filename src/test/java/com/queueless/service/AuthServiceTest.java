package com.queueless.service;

import com.queueless.dto.auth.AuthResponse;
import com.queueless.dto.auth.LoginRequest;
import com.queueless.dto.auth.RegisterRequest;
import com.queueless.dto.auth.UserResponse;
import com.queueless.entity.Provider;
import com.queueless.entity.User;
import com.queueless.entity.enums.Role;
import com.queueless.exception.DuplicateResourceException;
import com.queueless.repository.ProviderRepository;
import com.queueless.repository.UserRepository;
import com.queueless.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User customerUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("Rushika", "rushika@example.com", "Password123", Role.CUSTOMER);
        loginRequest = new LoginRequest("rushika@example.com", "Password123");
        customerUser = new User("Rushika", "rushika@example.com", "encodedPassword", Role.CUSTOMER, true);
        customerUser.setId(1L);
    }

    @Test
    @DisplayName("Should successfully register a new customer")
    void shouldRegisterNewCustomer() {
        when(userRepository.existsByEmail("rushika@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(customerUser);

        UserResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("rushika@example.com");
        assertThat(response.getRole()).isEqualTo(Role.CUSTOMER);

        verify(providerRepository, never()).save(any(Provider.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when registering existing email")
    void shouldThrowExceptionWhenRegisteringExistingEmail() {
        when(userRepository.existsByEmail("rushika@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("User already exists with email");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should create provider record when registering user with PROVIDER role")
    void shouldCreateProviderWhenRegisteringProviderRole() {
        RegisterRequest providerRequest = new RegisterRequest("Dr. Smith", "smith@example.com", "Pass123", Role.PROVIDER);
        User providerUser = new User("Dr. Smith", "smith@example.com", "encodedPass", Role.PROVIDER, true);
        providerUser.setId(2L);

        when(userRepository.existsByEmail("smith@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Pass123")).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenReturn(providerUser);

        UserResponse response = authService.register(providerRequest);

        assertThat(response.getRole()).isEqualTo(Role.PROVIDER);
        verify(providerRepository, times(1)).save(any(Provider.class));
    }

    @Test
    @DisplayName("Should successfully login and return JWT token")
    void shouldLoginSuccessfully() {
        when(userRepository.findByEmail("rushika@example.com")).thenReturn(Optional.of(customerUser));
        when(jwtService.generateToken(any())).thenReturn("mockJwtToken");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mockJwtToken");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(86400L);
        assertThat(response.getUser().getEmail()).isEqualTo("rushika@example.com");

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when login authentication fails")
    void shouldThrowExceptionWhenAuthenticationFails() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }
}
