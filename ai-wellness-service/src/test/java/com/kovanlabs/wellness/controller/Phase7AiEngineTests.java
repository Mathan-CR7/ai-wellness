package com.kovanlabs.wellness.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kovanlabs.wellness.dto.ai.AIChatRequest;
import com.kovanlabs.wellness.dto.ai.AIChatResponse;
import com.kovanlabs.wellness.entity.AIConversationEntity;
import com.kovanlabs.wellness.entity.UserEntity;
import com.kovanlabs.wellness.entity.enums.UserRole;
import com.kovanlabs.wellness.repository.UserRepository;
import com.kovanlabs.wellness.security.JwtTokenProvider;
import com.kovanlabs.wellness.service.AiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Phase7AiEngineTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiService aiService;

    private String userToken;
    private Long userId;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        UserEntity user = UserEntity.builder()
                .email("ai.test@example.com")
                .passwordHash("hashedpassword")
                .fullName("AI Tester")
                .role(UserRole.ROLE_USER)
                .build();
        user = userRepository.save(user);
        userId = user.getId();

        userToken = tokenProvider.generateToken(user.getEmail());

        AIChatResponse mockResponse = AIChatResponse.builder()
                .conversationId(101L)
                .messageId(201L)
                .content("AI Response: You are doing great!")
                .timestamp(Instant.now())
                .build();

        when(aiService.processChat(eq(userId), any(AIChatRequest.class))).thenReturn(mockResponse);

        AIConversationEntity mockConv = AIConversationEntity.builder()
                .id(101L)
                .userId(userId)
                .title("Hello AI")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(aiService.getUserConversations(eq(userId))).thenReturn(List.of(mockConv));
    }

    @Test
    void testChatWithFallback() throws Exception {
        AIChatRequest request = new AIChatRequest();
        request.setPrompt("How am I doing today?");

        mockMvc.perform(post("/api/ai/chat")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.conversationId", notNullValue()))
                .andExpect(jsonPath("$.messageId", notNullValue()));
    }

    @Test
    void testGetUserConversations() throws Exception {
        // Get conversations
        mockMvc.perform(get("/api/ai/conversations")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].title", containsString("Hello AI")));
    }
}
