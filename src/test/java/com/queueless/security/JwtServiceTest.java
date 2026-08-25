package com.queueless.security;

import com.queueless.entity.User;
import com.queueless.entity.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 86400000L);

        User user = new User("Test User", "test@example.com", "password", Role.CUSTOMER, true);
        user.setId(10L);
        userDetails = new CustomUserDetails(user);
    }

    @Test
    @DisplayName("Should generate valid JWT token and extract username")
    void shouldGenerateTokenAndExtractUsername() {
        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotBlank();
        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should validate token against user details")
    void shouldValidateToken() {
        String token = jwtService.generateToken(userDetails);
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertThat(isValid).isTrue();
    }
}
