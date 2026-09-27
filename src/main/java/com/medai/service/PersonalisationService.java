package com.medai.service;

import com.medai.model.User;
import com.medai.model.UserInteraction;
import com.medai.model.UserPreference;
import com.medai.repository.UserInteractionRepository;
import com.medai.repository.UserPreferenceRepository;
import com.medai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalisationService {

    private final UserInteractionRepository interactionRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final OllamaService ollamaService;

    /**
     * Toggle personalisation on/off for a user.
     */
    @Transactional
    public boolean togglePersonalisation(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean newState = !Boolean.TRUE.equals(user.getPersonalisation());
        user.setPersonalisation(newState);
        userRepository.save(user);
        return newState;
    }

    /**
     * Get personalisation status for a user.
     */
    public boolean getPersonalisationStatus(Long userId) {
        return userRepository.findById(userId)
                .map(u -> Boolean.TRUE.equals(u.getPersonalisation()))
                .orElse(false);
    }

    /**
     * Build a personalisation context string to inject into system prompts.
     * Uses vector similarity search to find relevant past interactions.
     */
    public String buildPersonalisationContext(Long userId, String currentMessage) {
        try {
            // Generate embedding for current message
            float[] queryEmbedding = ollamaService.generateEmbedding(currentMessage);
            String embeddingStr = toPostgresVector(queryEmbedding);

            // Find top-5 similar past interactions
            List<UserInteraction> similar = interactionRepository.findSimilarInteractions(
                    userId, embeddingStr, 5);

            if (similar.isEmpty()) {
                return buildPreferenceContext(userId);
            }

            StringBuilder context = new StringBuilder();
            context.append("Relevant patient history:\n");
            for (UserInteraction interaction : similar) {
                context.append("- [").append(interaction.getFeature()).append("] ");
                context.append(interaction.getInputSummary());
                if (interaction.getOutputSummary() != null) {
                    context.append(" → ").append(interaction.getOutputSummary());
                }
                context.append("\n");
            }

            // Append preferences
            context.append(buildPreferenceContext(userId));
            return context.toString();

        } catch (Exception e) {
            log.warn("Failed to build personalisation context: {}", e.getMessage());
            return buildPreferenceContext(userId);
        }
    }

    /**
     * Log a user interaction and store its embedding.
     */
    @Transactional
    public void logInteraction(Long userId, String feature, String inputSummary, String outputSummary) {
        try {
            float[] embedding = ollamaService.generateEmbedding(inputSummary);
            String embeddingStr = toPostgresVector(embedding);

            interactionRepository.insertWithEmbedding(
                    userId,
                    feature,
                    truncate(inputSummary, 500),
                    truncate(outputSummary, 500),
                    embeddingStr,
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            log.warn("Embedding generation failed, saving interaction without embedding: {}", e.getMessage());
            try {
                interactionRepository.insertWithoutEmbedding(
                        userId,
                        feature,
                        truncate(inputSummary, 500),
                        truncate(outputSummary, 500),
                        LocalDateTime.now()
                );
            } catch (Exception e2) {
                log.error("Failed to log interaction for user {}: {}", userId, e2.getMessage());
            }
        }
    }

    /**
     * Get a user's past interactions.
     */
    public List<UserInteraction> getInteractionHistory(Long userId, int limit) {
        return interactionRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, limit));
    }

    /**
     * Update user preferences based on extracted data.
     */
    @Transactional
    public void updatePreferences(Long userId, String[] knownConditions,
                                   String[] medications, String[] allergies) {
        User user = userRepository.getReferenceById(userId);
        var pref = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> UserPreference.builder().user(user).build());

        if (knownConditions != null) pref.setKnownConditions(knownConditions);
        if (medications != null) pref.setMedicationHist(medications);
        if (allergies != null) pref.setAllergies(allergies);
        pref.setUpdatedAt(LocalDateTime.now());
        preferenceRepository.save(pref);
    }

    private String buildPreferenceContext(Long userId) {
        return preferenceRepository.findByUserId(userId)
                .map(pref -> {
                    StringBuilder sb = new StringBuilder();
                    if (pref.getKnownConditions() != null && pref.getKnownConditions().length > 0) {
                        sb.append("Known conditions: ").append(String.join(", ", pref.getKnownConditions())).append("\n");
                    }
                    if (pref.getMedicationHist() != null && pref.getMedicationHist().length > 0) {
                        sb.append("Current medications: ").append(String.join(", ", pref.getMedicationHist())).append("\n");
                    }
                    if (pref.getAllergies() != null && pref.getAllergies().length > 0) {
                        sb.append("Allergies: ").append(String.join(", ", pref.getAllergies())).append("\n");
                    }
                    return sb.toString();
                })
                .orElse("");
    }

    private String toPostgresVector(float[] embedding) {
        String values = new StringBuilder("[")
                .append(arrayToString(embedding))
                .append("]")
                .toString();
        return values;
    }

    private String arrayToString(float[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
