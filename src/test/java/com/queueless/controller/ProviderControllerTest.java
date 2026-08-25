package com.queueless.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueless.dto.provider.ProviderResponse;
import com.queueless.entity.enums.ProviderStatus;
import com.queueless.repository.UserRepository;
import com.queueless.security.CustomUserDetailsService;
import com.queueless.security.JwtService;
import com.queueless.service.ProviderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProviderController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProviderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProviderService providerService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("GET /api/providers should return paginated providers list")
    void getAllProvidersShouldReturnPage() throws Exception {
        ProviderResponse res = new ProviderResponse(1L, 10L, "Dr. Smith", "smith@example.com", "Dentist", "Clinic", ProviderStatus.ACTIVE, LocalDateTime.now());

        when(providerService.getAllProviders(eq(ProviderStatus.ACTIVE), eq("Dentist"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(res)));

        mockMvc.perform(get("/api/providers")
                        .param("status", "ACTIVE")
                        .param("specialization", "Dentist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].specialization").value("Dentist"));
    }

    @Test
    @DisplayName("GET /api/providers/{id} should return provider details")
    void getProviderByIdShouldReturnProvider() throws Exception {
        ProviderResponse res = new ProviderResponse(1L, 10L, "Dr. Smith", "smith@example.com", "Dentist", "Clinic", ProviderStatus.ACTIVE, LocalDateTime.now());

        when(providerService.getProviderById(1L)).thenReturn(res);

        mockMvc.perform(get("/api/providers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userEmail").value("smith@example.com"));
    }
}
