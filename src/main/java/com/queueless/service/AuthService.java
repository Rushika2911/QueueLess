package com.queueless.service;

import com.queueless.dto.auth.AuthResponse;
import com.queueless.dto.auth.LoginRequest;
import com.queueless.dto.auth.RegisterRequest;
import com.queueless.dto.auth.UserResponse;
import com.queueless.entity.Provider;
import com.queueless.entity.User;
import com.queueless.entity.enums.ProviderStatus;
import com.queueless.entity.enums.Role;
import com.queueless.exception.DuplicateResourceException;
import com.queueless.exception.ResourceNotFoundException;
import com.queueless.repository.ProviderRepository;
import com.queueless.repository.UserRepository;
import com.queueless.security.CustomUserDetails;
import com.queueless.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            ProviderRepository providerRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User already exists with email: " + request.getEmail());
        }

        Role role = request.getRole() != null ? request.getRole() : Role.CUSTOMER;

        User user = new User(
                request.getName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                role,
                true
        );

        User savedUser = userRepository.save(user);

        if (role == Role.PROVIDER) {
            Provider provider = new Provider(
                    savedUser,
                    null,
                    null,
                    ProviderStatus.INACTIVE
            );
            providerRepository.save(provider);
        }

        return UserResponse.fromEntity(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String jwtToken = jwtService.generateToken(userDetails);
        long expiresInSeconds = jwtService.getExpirationMs() / 1000;

        return new AuthResponse(
                jwtToken,
                expiresInSeconds,
                UserResponse.fromEntity(user)
        );
    }
}
