package com.medai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medai.config.OllamaConfig;
import com.medai.dto.ConfidenceUpdate;
import com.medai.model.ChatMessage;
import com.medai.model.ChatSession;
import com.medai.model.ConfidenceScore;
import com.medai.repository.ConfidenceScoreRepository;
import com.medai.util.PromptTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfidenceEngine {

    private final OllamaService ollamaService;
    private final ConfidenceScoreRepository confidenceScoreRepository;
    private final OllamaConfig ollamaConfig;
    private final ObjectMapper objectMapper;

    /**
     * After each bot response, ask the model to re-evaluate confidence scores.
     * Persists updates to the database and returns the updated ConfidenceUpdate DTO.
     */
    @Transactional
    public ConfidenceUpdate updateConfidence(ChatSession session, List<Message> conversationHistory, String botType) {
        String modelName = resolveModel(botType);

        // Build conversation string for extraction prompt
        StringBuilder convText = new StringBuilder();
        for (Message msg : conversationHistory) {
            convText.append(msg.getMessageType().name()).append(": ")
                    .append(msg.getText()).append("\n");
        }

        String extractionPrompt = convText + "\n\n" + PromptTemplates.CONFIDENCE_EXTRACTION_PROMPT;

        try {
            String jsonResponse = ollamaService.query(modelName, extractionPrompt);
            // Strip markdown code fences if present
            jsonResponse = jsonResponse.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

            List<Map<String, Object>> scores = objectMapper.readValue(
                    jsonResponse, new TypeReference<>() {});

            List<ConfidenceUpdate.ConditionScore> conditionScores = new ArrayList<>();
            String topCondition = null;
            double topConfidence = 0.0;

            for (Map<String, Object> score : scores) {
                String condition = (String) score.get("condition");
                double confidence = ((Number) score.get("confidence")).doubleValue();
                String reasoning = (String) score.getOrDefault("reasoning", "");

                conditionScores.add(ConfidenceUpdate.ConditionScore.builder()
                        .condition(condition)
                        .confidence(confidence)
                        .reasoning(reasoning)
                        .build());

                // Upsert into DB
                var existing = confidenceScoreRepository.findBySessionIdAndConditionName(
                        session.getId(), condition);

                if (existing.isPresent()) {
                    ConfidenceScore cs = existing.get();
                    cs.setConfidencePct(BigDecimal.valueOf(confidence));
                    cs.setReasoning(reasoning);
                    cs.setUpdatedAt(LocalDateTime.now());
                    confidenceScoreRepository.save(cs);
                } else {
                    ConfidenceScore cs = ConfidenceScore.builder()
                            .session(session)
                            .conditionName(condition)
                            .confidencePct(BigDecimal.valueOf(confidence))
                            .reasoning(reasoning)
                            .build();
                    confidenceScoreRepository.save(cs);
                }

                if (confidence > topConfidence) {
                    topConfidence = confidence;
                    topCondition = condition;
                }
            }

            int threshold = resolveThreshold(botType);
            boolean thresholdReached = topConfidence >= threshold;

            return ConfidenceUpdate.builder()
                    .sessionId(session.getId())
                    .scores(conditionScores)
                    .topCondition(topCondition)
                    .topConfidence(topConfidence)
                    .thresholdReached(thresholdReached)
                    .build();

        } catch (Exception e) {
            log.warn("Could not parse confidence JSON from model response: {}", e.getMessage());
            return ConfidenceUpdate.builder()
                    .sessionId(session.getId())
                    .scores(List.of())
                    .thresholdReached(false)
                    .build();
        }
    }

    public List<ConfidenceScore> getScoresForSession(Long sessionId) {
        return confidenceScoreRepository.findBySessionIdOrderByConfidencePctDesc(sessionId);
    }

    private String resolveModel(String botType) {
        return switch (botType.toUpperCase()) {
            case "COMPLEX_DISEASE" -> ollamaConfig.getComplexDiseaseModel();
            default -> ollamaConfig.getCommonDiseaseModel();
        };
    }

    private int resolveThreshold(String botType) {
        return switch (botType.toUpperCase()) {
            case "COMPLEX_DISEASE" -> ollamaConfig.getThresholdComplex();
            case "MENTAL_HEALTH" -> ollamaConfig.getThresholdMental();
            default -> ollamaConfig.getThresholdCommon();
        };
    }
}
