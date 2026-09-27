package com.medai.service;

import com.medai.dto.ChatResponse;
import com.medai.model.User;
import com.medai.util.PromptTemplates;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class MedicineService {

    private final ChatService chatService;
    private final OllamaService ollamaService;

    /**
     * Conversational mode — maintains session history.
     */
    public ChatResponse chat(User user, String message, Long sessionId) {
        return chatService.processMessage(
                user, message, sessionId,
                "MEDICINE",
                PromptTemplates.MEDICINE_SYSTEM
        );
    }

    /**
     * Single-ask mode — no session history, one-shot response.
     */
    public String singleQuery(String question) {
        return ollamaService.chat(
                "qwen3.5:latest",
                PromptTemplates.MEDICINE_SYSTEM,
                java.util.List.of(),
                question
        );
    }

    public Flux<String> stream(User user, String message, Long sessionId) {
        return chatService.streamMessage(
                user, message, sessionId,
                "MEDICINE",
                PromptTemplates.MEDICINE_SYSTEM
        );
    }
}
