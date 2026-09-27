package com.medai.service;

import com.medai.dto.ChatResponse;
import com.medai.dto.ConfidenceUpdate;
import com.medai.model.*;
import com.medai.repository.ChatMessageRepository;
import com.medai.repository.ChatSessionRepository;
import com.medai.util.PromptTemplates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService Tests")
class ChatServiceTest {

    @Mock private ChatSessionRepository sessionRepository;
    @Mock private ChatMessageRepository messageRepository;
    @Mock private OllamaService ollamaService;
    @Mock private ConfidenceEngine confidenceEngine;
    @Mock private PersonalisationService personalisationService;

    @InjectMocks
    private ChatService chatService;

    private User user;
    private ChatSession session;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).username("testuser").email("test@test.com")
                .personalisation(false) // off by default for unit tests
                .build();

        session = ChatSession.builder()
                .id(5L).user(user).botType("COMMON_DISEASE").isActive(true).build();
    }

    @Test
    @DisplayName("Should create new session when sessionId is null")
    void processMessage_createsNewSession_whenSessionIdNull() {
        when(sessionRepository.findTopByUserIdAndBotTypeAndIsActiveTrueOrderByStartedAtDesc(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        when(sessionRepository.save(any())).thenReturn(session);
        when(messageRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(messageRepository.findLastNMessagesByUserAndBotType(anyLong(), anyString(), any(Pageable.class)))
                .thenReturn(List.of());
        when(ollamaService.chat(anyString(), anyString(), anyList(), anyString()))
                .thenReturn("I'll ask you some questions.");
        when(ollamaService.toMessage(anyString(), anyString()))
                .thenAnswer(i -> org.springframework.ai.chat.messages.UserMessage.builder()
                        .text((String) i.getArgument(1)).build());
        when(messageRepository.countBySessionId(anyLong())).thenReturn(1L);
        when(confidenceEngine.updateConfidence(any(), anyList(), anyString()))
                .thenReturn(ConfidenceUpdate.builder().scores(List.of()).thresholdReached(false).build());
        doNothing().when(personalisationService).logInteraction(anyLong(), anyString(), anyString(), anyString());

        ChatResponse response = chatService.processMessage(
                user, "I have a headache", null, "COMMON_DISEASE", PromptTemplates.COMMON_DISEASE_SYSTEM);

        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isEqualTo(5L);
        assertThat(response.getMessage()).isEqualTo("I'll ask you some questions.");
        verify(sessionRepository).save(any(ChatSession.class));
    }

    @Test
    @DisplayName("Should reuse existing session when valid sessionId is provided")
    void processMessage_reusesExistingSession_whenSessionIdProvided() {
        when(sessionRepository.findById(5L)).thenReturn(Optional.of(session));
        when(messageRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(messageRepository.findLastNMessagesByUserAndBotType(anyLong(), anyString(), any(Pageable.class)))
                .thenReturn(List.of());
        when(ollamaService.chat(anyString(), anyString(), anyList(), anyString()))
                .thenReturn("How long have you had the headache?");
        when(ollamaService.toMessage(anyString(), anyString()))
                .thenAnswer(i -> org.springframework.ai.chat.messages.UserMessage.builder()
                        .text((String) i.getArgument(1)).build());
        when(messageRepository.countBySessionId(anyLong())).thenReturn(2L);
        when(confidenceEngine.updateConfidence(any(), anyList(), anyString()))
                .thenReturn(ConfidenceUpdate.builder().scores(List.of()).thresholdReached(false).build());
        doNothing().when(personalisationService).logInteraction(anyLong(), anyString(), anyString(), anyString());

        ChatResponse response = chatService.processMessage(
                user, "Worse in the morning", 5L, "COMMON_DISEASE", PromptTemplates.COMMON_DISEASE_SYSTEM);

        assertThat(response.getSessionId()).isEqualTo(5L);
        // Should NOT create a new session
        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should inject personalisation context when personalisation is enabled")
    void processMessage_injectsPersonalisationContext_whenEnabled() {
        user.setPersonalisation(true);
        when(personalisationService.buildPersonalisationContext(anyLong(), anyString()))
                .thenReturn("Known conditions: hypertension");
        when(sessionRepository.findById(5L)).thenReturn(Optional.of(session));
        when(messageRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(messageRepository.findLastNMessagesByUserAndBotType(anyLong(), anyString(), any(Pageable.class)))
                .thenReturn(List.of());
        when(ollamaService.chat(anyString(), contains("hypertension"), anyList(), anyString()))
                .thenReturn("Noted. Does your headache come with blurred vision?");
        when(ollamaService.toMessage(anyString(), anyString()))
                .thenAnswer(i -> org.springframework.ai.chat.messages.UserMessage.builder()
                        .text((String) i.getArgument(1)).build());
        when(messageRepository.countBySessionId(anyLong())).thenReturn(2L);
        when(confidenceEngine.updateConfidence(any(), anyList(), anyString()))
                .thenReturn(ConfidenceUpdate.builder().scores(List.of()).thresholdReached(false).build());
        doNothing().when(personalisationService).logInteraction(anyLong(), anyString(), anyString(), anyString());

        ChatResponse response = chatService.processMessage(
                user, "My head hurts", 5L, "COMMON_DISEASE", PromptTemplates.COMMON_DISEASE_SYSTEM);

        assertThat(response.isPersonalised()).isTrue();
        verify(ollamaService).chat(anyString(), contains("hypertension"), anyList(), anyString());
    }

    @Test
    @DisplayName("Should trigger hospital finder for high-urgency conditions")
    void processMessage_triggerHospitalFinder_forHighUrgency() {
        ConfidenceUpdate highUrgency = ConfidenceUpdate.builder()
                .scores(List.of(ConfidenceUpdate.ConditionScore.builder()
                        .condition("heart disease").confidence(88).build()))
                .topCondition("heart disease")
                .topConfidence(88.0)
                .thresholdReached(true)
                .build();

        when(sessionRepository.findById(5L)).thenReturn(Optional.of(session));
        when(messageRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(messageRepository.findLastNMessagesByUserAndBotType(anyLong(), anyString(), any(Pageable.class)))
                .thenReturn(List.of());
        when(ollamaService.chat(anyString(), anyString(), anyList(), anyString()))
                .thenReturn("Based on your symptoms, I'm concerned about your heart. Please seek urgent care.");
        when(ollamaService.toMessage(anyString(), anyString()))
                .thenAnswer(i -> org.springframework.ai.chat.messages.UserMessage.builder()
                        .text((String) i.getArgument(1)).build());
        when(messageRepository.countBySessionId(anyLong())).thenReturn(6L);
        when(confidenceEngine.updateConfidence(any(), anyList(), anyString())).thenReturn(highUrgency);
        doNothing().when(personalisationService).logInteraction(anyLong(), anyString(), anyString(), anyString());

        ChatResponse response = chatService.processMessage(
                user, "severe chest pain", 5L, "COMPLEX_DISEASE", PromptTemplates.COMPLEX_DISEASE_SYSTEM);

        assertThat(response.isThresholdReached()).isTrue();
        assertThat(response.isTriggerHospitalFinder()).isTrue();
        assertThat(response.getHospitalSpecialtyFilter()).isEqualTo("Cardiology");
    }

    @Test
    @DisplayName("Should skip confidence update for MEDICINE bot type")
    void processMessage_skipsConfidenceUpdate_forMedicine() {
        when(sessionRepository.findTopByUserIdAndBotTypeAndIsActiveTrueOrderByStartedAtDesc(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        when(sessionRepository.save(any())).thenReturn(ChatSession.builder().id(7L).user(user).botType("MEDICINE").build());
        when(messageRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(messageRepository.findLastNMessagesByUserAndBotType(anyLong(), anyString(), any(Pageable.class)))
                .thenReturn(List.of());
        when(ollamaService.chat(anyString(), anyString(), anyList(), anyString()))
                .thenReturn("Ibuprofen is a nonsteroidal anti-inflammatory drug...");
        when(ollamaService.toMessage(anyString(), anyString()))
                .thenAnswer(i -> org.springframework.ai.chat.messages.UserMessage.builder()
                        .text((String) i.getArgument(1)).build());
        when(messageRepository.countBySessionId(anyLong())).thenReturn(1L);
        doNothing().when(personalisationService).logInteraction(anyLong(), anyString(), anyString(), anyString());

        chatService.processMessage(user, "What is ibuprofen?", null, "MEDICINE", PromptTemplates.MEDICINE_SYSTEM);

        // ConfidenceEngine must NOT be called for medicine bot
        verify(confidenceEngine, never()).updateConfidence(any(), anyList(), anyString());
    }

    @Test
    @DisplayName("Should return session messages in chronological order")
    void getSessionMessages_returnsChronologicalMessages() {
        List<ChatMessage> messages = List.of(
                ChatMessage.builder().role("USER").content("Hello").build(),
                ChatMessage.builder().role("ASSISTANT").content("Hi, how can I help?").build()
        );
        when(messageRepository.findBySessionIdOrderByTimestampAsc(5L)).thenReturn(messages);

        var result = chatService.getSessionMessages(5L);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRole()).isEqualTo("USER");
    }
}
