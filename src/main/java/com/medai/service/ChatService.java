package com.medai.service;

import com.medai.dto.ChatResponse;
import com.medai.dto.ConfidenceUpdate;
import com.medai.model.ChatMessage;
import com.medai.model.ChatSession;
import com.medai.model.User;
import com.medai.repository.ChatMessageRepository;
import com.medai.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final OllamaService ollamaService;
    private final ConfidenceEngine confidenceEngine;
    private final PersonalisationService personalisationService;

    private static final int HISTORY_WINDOW = 20; // last N messages loaded for context

    /**
     * Main entry point for all chatbot interactions.
     * Handles session management, history loading, personalisation, and confidence tracking.
     */
    @Transactional
    public ChatResponse processMessage(User user, String userMessage, Long sessionId,
                                        String botType, String systemPrompt) {
        // 1. Resolve or create session
        ChatSession session = resolveSession(user, sessionId, botType);

        // 2. Persist user message
        persistMessage(session, "USER", userMessage);

        // 3. Load cross-session conversation history
        List<Message> history = loadConversationHistory(user.getId(), botType, session.getId());

        // 4. Optionally inject personalisation context into system prompt
        String enrichedSystemPrompt = systemPrompt;
        boolean personalised = false;
        if (Boolean.TRUE.equals(user.getPersonalisation())) {
            String ctx = personalisationService.buildPersonalisationContext(user.getId(), userMessage);
            if (!ctx.isBlank()) {
                enrichedSystemPrompt = systemPrompt + "\n\nPATIENT CONTEXT:\n" + ctx;
                personalised = true;
            }
        }

        // 5. Call Ollama
        String botResponse = ollamaService.chat(resolveModel(botType), enrichedSystemPrompt, history, userMessage);

        // 6. Persist assistant response
        persistMessage(session, "ASSISTANT", botResponse);

        // 7. Update confidence scores (only for disease/mental health bots, not medicine)
        ConfidenceUpdate confidenceUpdate = null;
        if (!botType.equalsIgnoreCase("MEDICINE")) {
            List<Message> updatedHistory = loadConversationHistory(user.getId(), botType, session.getId());
            confidenceUpdate = confidenceEngine.updateConfidence(session, updatedHistory, botType);
        }

        // 8. Log interaction for personalisation engine
        personalisationService.logInteraction(
                user.getId(), botType,
                truncate(userMessage, 300),
                truncate(botResponse, 300)
        );

        // 9. Build response
        boolean triggerHospital = confidenceUpdate != null &&
                confidenceUpdate.isThresholdReached() &&
                isHighUrgencyCondition(confidenceUpdate.getTopCondition());

        long msgCount = messageRepository.countBySessionId(session.getId());

        return ChatResponse.builder()
                .sessionId(session.getId())
                .botType(botType)
                .message(botResponse)
                .role("ASSISTANT")
                .confidenceScores(confidenceUpdate != null ? confidenceUpdate.getScores() : List.of())
                .topCondition(confidenceUpdate != null ? confidenceUpdate.getTopCondition() : null)
                .topConfidence(confidenceUpdate != null ? confidenceUpdate.getTopConfidence() : null)
                .thresholdReached(confidenceUpdate != null && confidenceUpdate.isThresholdReached())
                .triggerHospitalFinder(triggerHospital)
                .hospitalSpecialtyFilter(triggerHospital ? mapConditionToSpecialty(confidenceUpdate.getTopCondition()) : null)
                .personalised(personalised)
                .messageCount(msgCount)
                .build();
    }

    /**
     * Stream chat response via SSE (Flux<String>).
     */
    @Transactional
    public Flux<String> streamMessage(User user, String userMessage, Long sessionId,
                                       String botType, String systemPrompt) {
        ChatSession session = resolveSession(user, sessionId, botType);
        persistMessage(session, "USER", userMessage);

        List<Message> history = loadConversationHistory(user.getId(), botType, session.getId());

        String enrichedSystemPrompt = systemPrompt;
        if (Boolean.TRUE.equals(user.getPersonalisation())) {
            String ctx = personalisationService.buildPersonalisationContext(user.getId(), userMessage);
            if (!ctx.isBlank()) {
                enrichedSystemPrompt = systemPrompt + "\n\nPATIENT CONTEXT:\n" + ctx;
            }
        }

        final String finalSystemPrompt = enrichedSystemPrompt;
        final ChatSession finalSession = session;
        final StringBuilder fullResponse = new StringBuilder();

        return ollamaService.streamChat(resolveModel(botType), finalSystemPrompt, history, userMessage)
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    persistMessage(finalSession, "ASSISTANT", fullResponse.toString());
                    personalisationService.logInteraction(
                            user.getId(), botType,
                            truncate(userMessage, 300),
                            truncate(fullResponse.toString(), 300)
                    );
                });
    }

    /**
     * Get all sessions for a user, optionally filtered by bot type.
     */
    public List<ChatSession> getUserSessions(Long userId) {
        return sessionRepository.findByUserIdOrderByStartedAtDesc(userId);
    }

    /**
     * Get full conversation history for a specific session.
     */
    public List<ChatMessage> getSessionMessages(Long sessionId) {
        return messageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }

    // --- Private helpers ---

    private ChatSession resolveSession(User user, Long sessionId, String botType) {
        if (sessionId != null) {
            return sessionRepository.findById(sessionId)
                    .filter(s -> s.getUser().getId().equals(user.getId()))
                    .orElseGet(() -> createNewSession(user, botType));
        }
        return createNewSession(user, botType);
    }

    private ChatSession createNewSession(User user, String botType) {
        // Close any previous active sessions of same type
        sessionRepository.findTopByUserIdAndBotTypeAndIsActiveTrueOrderByStartedAtDesc(
                user.getId(), botType).ifPresent(old -> {
            old.setIsActive(false);
            old.setEndedAt(LocalDateTime.now());
            sessionRepository.save(old);
        });

        ChatSession session = ChatSession.builder()
                .user(user)
                .botType(botType.toUpperCase())
                .startedAt(LocalDateTime.now())
                .isActive(true)
                .build();
        return sessionRepository.save(session);
    }

    private void persistMessage(ChatSession session, String role, String content) {
        ChatMessage msg = ChatMessage.builder()
                .session(session)
                .role(role)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
        messageRepository.save(msg);
    }

    private List<Message> loadConversationHistory(Long userId, String botType, Long currentSessionId) {
        // Load last HISTORY_WINDOW messages across all sessions of this bot type
        List<ChatMessage> recentMessages = messageRepository.findLastNMessagesByUserAndBotType(
                userId, botType.toUpperCase(), PageRequest.of(0, HISTORY_WINDOW));

        // Reverse to get chronological order
        List<ChatMessage> chronological = new java.util.ArrayList<>(recentMessages);
        java.util.Collections.reverse(chronological);

        return chronological.stream()
                .filter(m -> !m.getRole().equals("SYSTEM"))
                .map(m -> ollamaService.toMessage(m.getRole(), m.getContent()))
                .collect(Collectors.toList());
    }

    private String resolveModel(String botType) {
        return switch (botType.toUpperCase()) {
            case "MENTAL_HEALTH" -> "medllama2";
            case "COMMON_DISEASE" -> "medllama2";
            case "COMPLEX_DISEASE" -> "meditron";
            case "MEDICINE" -> "qwen3.5:latest";
            default -> "llama3.1:8b";
        };
    }

    private boolean isHighUrgencyCondition(String condition) {
        if (condition == null) return false;
        String c = condition.toLowerCase();
        return c.contains("heart") || c.contains("stroke") || c.contains("cancer") ||
               c.contains("kidney") || c.contains("liver") || c.contains("emergency") ||
               c.contains("coronary") || c.contains("diabetes") || c.contains("tumor");
    }

    private String mapConditionToSpecialty(String condition) {
        if (condition == null) return "General Medicine";
        String c = condition.toLowerCase();
        if (c.contains("heart") || c.contains("coronary")) return "Cardiology";
        if (c.contains("cancer") || c.contains("tumor")) return "Oncology";
        if (c.contains("diabetes")) return "Endocrinology";
        if (c.contains("kidney")) return "Nephrology";
        if (c.contains("liver")) return "Hepatology";
        if (c.contains("lung") || c.contains("copd")) return "Pulmonology";
        if (c.contains("stroke")) return "Neurology";
        return "General Medicine";
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
