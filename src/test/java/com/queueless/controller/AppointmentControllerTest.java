package com.queueless.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueless.dto.appointment.AppointmentResponse;
import com.queueless.dto.appointment.CreateAppointmentRequest;
import com.queueless.entity.enums.AppointmentStatus;
import com.queueless.exception.SlotUnavailableException;
import com.queueless.repository.UserRepository;
import com.queueless.security.CustomUserDetailsService;
import com.queueless.security.JwtService;
import com.queueless.service.AppointmentService;
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
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AppointmentService appointmentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("POST /api/appointments should return 201 Created on valid booking request")
    void bookAppointmentShouldReturn201() throws Exception {
        LocalDate date = LocalDate.now().plusDays(1);
        CreateAppointmentRequest request = new CreateAppointmentRequest(10L, 100L, date, LocalTime.of(10, 0));
        AppointmentResponse response = new AppointmentResponse(500L, 1L, "Rushika", "rushika@example.com", 10L, "Dr. Smith", 100L, "Cleaning", date, LocalTime.of(10, 0), LocalTime.of(10, 30), AppointmentStatus.BOOKED, LocalDateTime.now());

        when(appointmentService.bookAppointment(any(CreateAppointmentRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(500))
                .andExpect(jsonPath("$.status").value("BOOKED"));
    }

    @Test
    @DisplayName("POST /api/appointments should return 409 Conflict when slot is unavailable")
    void bookAppointmentShouldReturn409WhenSlotUnavailable() throws Exception {
        LocalDate date = LocalDate.now().plusDays(1);
        CreateAppointmentRequest request = new CreateAppointmentRequest(10L, 100L, date, LocalTime.of(10, 0));

        when(appointmentService.bookAppointment(any(CreateAppointmentRequest.class), any()))
                .thenThrow(new SlotUnavailableException("The requested appointment slot is no longer available"));

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SLOT_UNAVAILABLE"));
    }
}
