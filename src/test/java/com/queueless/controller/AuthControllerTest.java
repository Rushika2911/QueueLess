package com.queueless.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueless.dto.auth.AuthResponse;
import com.queueless.dto.auth.LoginRequest;
import com.queueless.dto.auth.RegisterRequest;
import com.queueless.dto.auth.UserResponse;
import com.queueless.entity.enums.Role;
import com.queueless.exception.DuplicateResourceException;
import com.queueless.repository.UserRepository;
import com.queueless.security.CustomUserDetailsService;
import com.queueless.security.JwtService;
import com.queueless.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("POST /api/auth/register should return 201 Created on valid input")
    void registerShouldReturn201Created() throws Exception {
        RegisterRequest request = new RegisterRequest("Rushika", "rushika@example.com", "Password123", Role.CUSTOMER);
        UserResponse response = new UserResponse(1L, "Rushika", "rushika@example.com", Role.CUSTOMER, true, LocalDateTime.now());

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Rushika"))
                .andExpect(jsonPath("$.email").value("rushika@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("POST /api/auth/register should return 400 Bad Request on invalid email")
    void registerShouldReturn400OnInvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("Rushika", "invalid-email", "Password123", Role.CUSTOMER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/auth/register should return 409 Conflict on duplicate email")
    void registerShouldReturn409OnDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("Rushika", "existing@example.com", "Password123", Role.CUSTOMER);

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("User already exists with email: existing@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("POST /api/auth/login should return 200 OK with access token")
    void loginShouldReturn200WithToken() throws Exception {
        LoginRequest request = new LoginRequest("rushika@example.com", "Password123");
        UserResponse userResponse = new UserResponse(1L, "Rushika", "rushika@example.com", Role.CUSTOMER, true, LocalDateTime.now());
        AuthResponse response = new AuthResponse("mockJwtToken", 86400L, userResponse);

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mockJwtToken"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400));
    }

    @Test
    @DisplayName("POST /api/auth/login should return 401 Unauthorized on invalid credentials")
    void loginShouldReturn401OnInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("rushika@example.com", "WrongPassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }
}
