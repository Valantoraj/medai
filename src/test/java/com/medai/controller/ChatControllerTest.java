package com.medai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medai.dto.ChatRequest;
import com.medai.dto.ChatResponse;
import com.medai.dto.ConfidenceUpdate;
import com.medai.model.ChatSession;
import com.medai.model.User;
import com.medai.service.*;
import com.medai.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@DisplayName("ChatController Tests")
class ChatControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private MentalHealthService mentalHealthService;
    @MockBean private CommonDiseaseService commonDiseaseService;
    @MockBean private ComplexDiseaseService complexDiseaseService;
    @MockBean private ChatService chatService;
    @MockBean private ConfidenceEngine confidenceEngine;
    @MockBean private AuthService authService;
    @MockBean private JwtUtil jwtUtil;

    private User testUser;
    private ChatResponse mockResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L).username("testuser").email("test@test.com")
                .passwordHash("hashed").personalisation(true).build();

        mockResponse = ChatResponse.builder()
                .sessionId(10L)
                .botType("MENTAL_HEALTH")
                .message("How are you feeling today?")
                .role("ASSISTANT")
                .confidenceScores(List.of())
                .thresholdReached(false)
                .triggerHospitalFinder(false)
                .personalised(false)
                .messageCount(1L)
                .build();

        // Make auth service return the test user for JWT validation
        when(authService.loadUserByUsername("testuser")).thenReturn(testUser);
    }

    // ── Mental Health ──────────────────────────────────────

    @Test
    @DisplayName("POST /api/chat/mental-health — 200 with valid message")
    @WithMockUser(username = "testuser")
    void mentalHealth_validMessage_returns200() throws Exception {
        when(mentalHealthService.chat(any(User.class), anyString(), any()))
                .thenReturn(mockResponse);

        ChatRequest req = new ChatRequest();
        req.setMessage("I've been feeling really anxious lately");
        req.setBotType("MENTAL_HEALTH");

        mockMvc.perform(post("/api/chat/mental-health")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(10))
                .andExpect(jsonPath("$.message").value("How are you feeling today?"))
                .andExpect(jsonPath("$.role").value("ASSISTANT"));
    }

    @Test
    @DisplayName("POST /api/chat/mental-health — 400 when message is blank")
    @WithMockUser(username = "testuser")
    void mentalHealth_blankMessage_returns400() throws Exception {
        ChatRequest req = new ChatRequest();
        req.setMessage("  ");  // blank
        req.setBotType("MENTAL_HEALTH");

        mockMvc.perform(post("/api/chat/mental-health")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── Common Disease ─────────────────────────────────────

    @Test
    @DisplayName("POST /api/chat/common-disease — 200 with valid message")
    @WithMockUser(username = "testuser")
    void commonDisease_validMessage_returns200() throws Exception {
        ChatResponse commonResponse = ChatResponse.builder()
                .sessionId(11L).botType("COMMON_DISEASE")
                .message("Where exactly is the pain?").role("ASSISTANT")
                .confidenceScores(List.of()).messageCount(1L).build();

        when(commonDiseaseService.chat(any(User.class), anyString(), any()))
                .thenReturn(commonResponse);

        ChatRequest req = new ChatRequest();
        req.setMessage("I have a headache and fever");
        req.setBotType("COMMON_DISEASE");

        mockMvc.perform(post("/api/chat/common-disease")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(11))
                .andExpect(jsonPath("$.botType").value("COMMON_DISEASE"));
    }

    @Test
    @DisplayName("POST /api/chat/common-disease — returns confidence scores when present")
    @WithMockUser(username = "testuser")
    void commonDisease_returnsConfidenceScores_whenPresent() throws Exception {
        List<ConfidenceUpdate.ConditionScore> scores = List.of(
                ConfidenceUpdate.ConditionScore.builder().condition("influenza").confidence(65).reasoning("fever").build(),
                ConfidenceUpdate.ConditionScore.builder().condition("cold").confidence(30).reasoning("mild").build()
        );

        ChatResponse responseWithScores = ChatResponse.builder()
                .sessionId(12L).botType("COMMON_DISEASE")
                .message("Do you have a runny nose?").role("ASSISTANT")
                .confidenceScores(scores).topCondition("influenza").topConfidence(65.0)
                .thresholdReached(false).messageCount(3L).build();

        when(commonDiseaseService.chat(any(User.class), anyString(), any()))
                .thenReturn(responseWithScores);

        ChatRequest req = new ChatRequest();
        req.setMessage("Yes, I have a runny nose too");
        req.setBotType("COMMON_DISEASE");

        mockMvc.perform(post("/api/chat/common-disease")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confidenceScores").isArray())
                .andExpect(jsonPath("$.confidenceScores[0].condition").value("influenza"))
                .andExpect(jsonPath("$.topCondition").value("influenza"))
                .andExpect(jsonPath("$.thresholdReached").value(false));
    }

    // ── Complex Disease ────────────────────────────────────

    @Test
    @DisplayName("POST /api/chat/complex-disease — triggers hospital finder on critical condition")
    @WithMockUser(username = "testuser")
    void complexDisease_triggersHospitalFinder_onCritical() throws Exception {
        ChatResponse criticalResponse = ChatResponse.builder()
                .sessionId(13L).botType("COMPLEX_DISEASE")
                .message("Your symptoms suggest possible cardiac involvement. Please seek immediate care.")
                .role("ASSISTANT").confidenceScores(List.of())
                .thresholdReached(true).triggerHospitalFinder(true)
                .hospitalSpecialtyFilter("Cardiology").messageCount(8L).build();

        when(complexDiseaseService.chat(any(User.class), anyString(), any()))
                .thenReturn(criticalResponse);

        ChatRequest req = new ChatRequest();
        req.setMessage("I have severe chest pain radiating to my arm");
        req.setBotType("COMPLEX_DISEASE");

        mockMvc.perform(post("/api/chat/complex-disease")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.triggerHospitalFinder").value(true))
                .andExpect(jsonPath("$.hospitalSpecialtyFilter").value("Cardiology"))
                .andExpect(jsonPath("$.thresholdReached").value(true));
    }

    // ── Session history ────────────────────────────────────

    @Test
    @DisplayName("GET /api/chat/sessions — 200 returns user sessions list")
    @WithMockUser(username = "testuser")
    void getSessions_returns200_withSessionList() throws Exception {
        List<ChatSession> sessions = List.of(
                ChatSession.builder().id(1L).botType("MENTAL_HEALTH").build(),
                ChatSession.builder().id(2L).botType("COMMON_DISEASE").build()
        );
        when(chatService.getUserSessions(anyLong())).thenReturn(sessions);

        mockMvc.perform(get("/api/chat/sessions").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/chat/confidence/{sessionId} — 200 returns confidence scores")
    @WithMockUser(username = "testuser")
    void getConfidenceScores_returns200_withScores() throws Exception {
        when(confidenceEngine.getScoresForSession(10L)).thenReturn(List.of());

        mockMvc.perform(get("/api/chat/confidence/10").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ── Auth guard ─────────────────────────────────────────

    @Test
    @DisplayName("POST /api/chat/mental-health — 401 without authentication")
    void mentalHealth_withoutAuth_returns401() throws Exception {
        ChatRequest req = new ChatRequest();
        req.setMessage("test");
        req.setBotType("MENTAL_HEALTH");

        mockMvc.perform(post("/api/chat/mental-health")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
