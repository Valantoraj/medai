package com.medai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medai.config.OllamaConfig;
import com.medai.model.ChatSession;
import com.medai.model.ConfidenceScore;
import com.medai.model.User;
import com.medai.repository.ConfidenceScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.UserMessage;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfidenceEngine Tests")
class ConfidenceEngineTest {

    @Mock private OllamaService ollamaService;
    @Mock private ConfidenceScoreRepository confidenceScoreRepository;
    @Mock private OllamaConfig ollamaConfig;

    @InjectMocks
    private ConfidenceEngine confidenceEngine;

    private ObjectMapper objectMapper;
    private ChatSession session;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Inject real ObjectMapper via reflection since @InjectMocks won't inject it
        try {
            var field = ConfidenceEngine.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(confidenceEngine, objectMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        User user = User.builder().id(1L).username("testuser").build();
        session = ChatSession.builder().id(10L).user(user).botType("COMMON_DISEASE").build();
    }

    @Test
    @DisplayName("Should parse valid JSON from Ollama and update confidence scores")
    void updateConfidence_parsesValidJson_updatesScores() {
        String validJson = "[{\"condition\":\"influenza\",\"confidence\":65,\"reasoning\":\"fever + cough\"},{\"condition\":\"common cold\",\"confidence\":30,\"reasoning\":\"mild symptoms\"}]";

        when(ollamaConfig.getCommonDiseaseModel()).thenReturn("medllama2");
        when(ollamaConfig.getThresholdCommon()).thenReturn(80);
        when(ollamaService.query(anyString(), anyString())).thenReturn(validJson);
        when(confidenceScoreRepository.findBySessionIdAndConditionName(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        when(confidenceScoreRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = confidenceEngine.updateConfidence(
                session, List.of(new UserMessage("I have a fever")), "COMMON_DISEASE");

        assertThat(result).isNotNull();
        assertThat(result.getScores()).hasSize(2);
        assertThat(result.getTopCondition()).isEqualTo("influenza");
        assertThat(result.getTopConfidence()).isEqualTo(65.0);
        assertThat(result.isThresholdReached()).isFalse(); // 65 < 80
        verify(confidenceScoreRepository, times(2)).save(any(ConfidenceScore.class));
    }

    @Test
    @DisplayName("Should detect threshold reached when confidence >= threshold")
    void updateConfidence_thresholdReached_whenConfidenceHigh() {
        String json = "[{\"condition\":\"pneumonia\",\"confidence\":85,\"reasoning\":\"chest pain + fever + cough\"}]";

        when(ollamaConfig.getCommonDiseaseModel()).thenReturn("medllama2");
        when(ollamaConfig.getThresholdCommon()).thenReturn(80);
        when(ollamaService.query(anyString(), anyString())).thenReturn(json);
        when(confidenceScoreRepository.findBySessionIdAndConditionName(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        when(confidenceScoreRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = confidenceEngine.updateConfidence(
                session, List.of(new UserMessage("Chest pain")), "COMMON_DISEASE");

        assertThat(result.isThresholdReached()).isTrue();
        assertThat(result.getTopCondition()).isEqualTo("pneumonia");
        assertThat(result.getTopConfidence()).isEqualTo(85.0);
    }

    @Test
    @DisplayName("Should use higher threshold for COMPLEX_DISEASE bot type")
    void updateConfidence_usesComplexThreshold_forComplexDisease() {
        String json = "[{\"condition\":\"coronary artery disease\",\"confidence\":83,\"reasoning\":\"family history + chest pain\"}]";

        ChatSession complexSession = ChatSession.builder().id(20L)
                .user(User.builder().id(1L).build()).botType("COMPLEX_DISEASE").build();

        when(ollamaConfig.getComplexDiseaseModel()).thenReturn("meditron");
        when(ollamaConfig.getThresholdComplex()).thenReturn(85);
        when(ollamaService.query(anyString(), anyString())).thenReturn(json);
        when(confidenceScoreRepository.findBySessionIdAndConditionName(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        when(confidenceScoreRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = confidenceEngine.updateConfidence(
                complexSession, List.of(new UserMessage("Chest tightness")), "COMPLEX_DISEASE");

        // 83 < 85 (complex threshold), should NOT be reached
        assertThat(result.isThresholdReached()).isFalse();
    }

    @Test
    @DisplayName("Should handle malformed JSON gracefully, returning empty scores")
    void updateConfidence_handlesInvalidJson_returnsEmpty() {
        when(ollamaConfig.getCommonDiseaseModel()).thenReturn("medllama2");
        when(ollamaConfig.getThresholdCommon()).thenReturn(80);
        when(ollamaService.query(anyString(), anyString())).thenReturn("not valid json at all");

        var result = confidenceEngine.updateConfidence(
                session, List.of(new UserMessage("test")), "COMMON_DISEASE");

        assertThat(result).isNotNull();
        assertThat(result.getScores()).isEmpty();
        assertThat(result.isThresholdReached()).isFalse();
        verify(confidenceScoreRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should upsert existing confidence score rather than creating duplicate")
    void updateConfidence_updatesExistingScore_ratherThanCreatingDuplicate() {
        String json = "[{\"condition\":\"flu\",\"confidence\":72,\"reasoning\":\"updated\"}]";
        ConfidenceScore existing = ConfidenceScore.builder()
                .id(5L).session(session).conditionName("flu")
                .confidencePct(BigDecimal.valueOf(50)).build();

        when(ollamaConfig.getCommonDiseaseModel()).thenReturn("medllama2");
        when(ollamaConfig.getThresholdCommon()).thenReturn(80);
        when(ollamaService.query(anyString(), anyString())).thenReturn(json);
        when(confidenceScoreRepository.findBySessionIdAndConditionName(10L, "flu"))
                .thenReturn(Optional.of(existing));
        when(confidenceScoreRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        confidenceEngine.updateConfidence(session, List.of(new UserMessage("worse")), "COMMON_DISEASE");

        // Verify the existing score was updated (id still 5)
        verify(confidenceScoreRepository).save(argThat(cs ->
                cs instanceof ConfidenceScore &&
                ((ConfidenceScore) cs).getConfidencePct().compareTo(BigDecimal.valueOf(72)) == 0
        ));
    }

    @Test
    @DisplayName("Should strip markdown code fences from JSON response")
    void updateConfidence_stripsMarkdownFences_beforeParsing() {
        String jsonWithFences = "```json\n[{\"condition\":\"migraine\",\"confidence\":55,\"reasoning\":\"headache\"}]\n```";

        when(ollamaConfig.getCommonDiseaseModel()).thenReturn("medllama2");
        when(ollamaConfig.getThresholdCommon()).thenReturn(80);
        when(ollamaService.query(anyString(), anyString())).thenReturn(jsonWithFences);
        when(confidenceScoreRepository.findBySessionIdAndConditionName(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        when(confidenceScoreRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = confidenceEngine.updateConfidence(
                session, List.of(new UserMessage("bad headache")), "COMMON_DISEASE");

        assertThat(result.getScores()).hasSize(1);
        assertThat(result.getTopCondition()).isEqualTo("migraine");
    }

    @Test
    @DisplayName("Should return scores for a given session from repository")
    void getScoresForSession_returnsFromRepository() {
        List<ConfidenceScore> expected = List.of(
                ConfidenceScore.builder().conditionName("flu").confidencePct(BigDecimal.valueOf(60)).build()
        );
        when(confidenceScoreRepository.findBySessionIdOrderByConfidencePctDesc(10L)).thenReturn(expected);

        var result = confidenceEngine.getScoresForSession(10L);
        assertThat(result).isEqualTo(expected);
    }
}
