package com.queueless.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueless.dto.queue.QueueEntryResponse;
import com.queueless.dto.queue.QueuePositionResponse;
import com.queueless.entity.enums.QueueStatus;
import com.queueless.exception.QueueConflictException;
import com.queueless.repository.UserRepository;
import com.queueless.security.CustomUserDetailsService;
import com.queueless.security.JwtService;
import com.queueless.service.QueueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QueueController.class)
@AutoConfigureMockMvc(addFilters = false)
class QueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QueueService queueService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("POST /api/queues/{providerId}/join should return 201 Created")
    void joinQueueShouldReturn201() throws Exception {
        QueueEntryResponse response = new QueueEntryResponse(100L, 10L, "Dr. Smith", 1L, "Rushika", null, LocalDate.now(), 1, 1, QueueStatus.WAITING, 0, LocalDateTime.now(), null, null);

        when(queueService.joinQueue(eq(10L), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/queues/10/join")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenNumber").value(1))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    @DisplayName("POST /api/queues/{providerId}/join should return 409 Conflict if active entry exists")
    void joinQueueShouldReturn409OnConflict() throws Exception {
        when(queueService.joinQueue(eq(10L), any(), any()))
                .thenThrow(new QueueConflictException("Customer already has an active queue entry for this provider today"));

        mockMvc.perform(post("/api/queues/10/join")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("QUEUE_CONFLICT"));
    }

    @Test
    @DisplayName("GET /api/queues/my-position should return 200 OK with queue position")
    void getMyPositionShouldReturnPosition() throws Exception {
        QueuePositionResponse response = new QueuePositionResponse(100L, 10L, "Dr. Smith", 1, 1, 0, 0, QueueStatus.WAITING);

        when(queueService.getMyPosition(any())).thenReturn(response);

        mockMvc.perform(get("/api/queues/my-position"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenNumber").value(1))
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.peopleAhead").value(0));
    }
}
