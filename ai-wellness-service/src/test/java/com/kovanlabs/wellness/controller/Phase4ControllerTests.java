package com.kovanlabs.wellness.controller;

import com.kovanlabs.wellness.dto.activity.ActivitySyncRequest;
import com.kovanlabs.wellness.dto.auth.LoginRequest;
import com.kovanlabs.wellness.dto.auth.UserRegistrationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Phase4ControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Test User Registration REST Endpoint")
    void testRegisterEndpoint() throws Exception {
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("api.user@example.com")
                .password("Password123!")
                .fullName("API Test User")
                .weightKg(70.0)
                .heightCm(175.0)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.email", is("api.user@example.com")))
                .andExpect(jsonPath("$.user.fullName", is("API Test User")));
    }

    @Test
    @DisplayName("Test User Login and JWT Token Generation")
    void testLoginEndpoint() throws Exception {
        UserRegistrationRequest registerReq = UserRegistrationRequest.builder()
                .email("login.user@example.com")
                .password("SecurePass123!")
                .fullName("Login User")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = LoginRequest.builder()
                .email("login.user@example.com")
                .password("SecurePass123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")));
    }

    @Test
    @DisplayName("Test Protected Endpoint Access With Bearer Token")
    void testProtectedEndpointAccess() throws Exception {
        UserRegistrationRequest registerReq = UserRegistrationRequest.builder()
                .email("protected.user@example.com")
                .password("SecurePass123!")
                .fullName("Protected User")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseJson).get("token").asText();

        // Access GET /api/users/me with Bearer token
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("protected.user@example.com")));
    }

    @Test
    @DisplayName("Test Health Connect Activity Sync via REST Endpoint")
    void testActivitySyncEndpoint() throws Exception {
        UserRegistrationRequest registerReq = UserRegistrationRequest.builder()
                .email("activity.user@example.com")
                .password("SecurePass123!")
                .fullName("Activity User")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();

        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);

        ActivitySyncRequest syncReq = ActivitySyncRequest.builder()
                .stepCount(8500)
                .distanceMeters(6200.0)
                .caloriesBurned(450.0)
                .startTime(start)
                .endTime(now)
                .sourceDevice("ANDROID_HEALTH_CONNECT")
                .build();

        mockMvc.perform(post("/api/activities/sync")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(syncReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stepCount", is(8500)))
                .andExpect(jsonPath("$.distanceMeters", is(6200.0)))
                .andExpect(jsonPath("$.sourceDevice", is("ANDROID_HEALTH_CONNECT")));
    }
}
