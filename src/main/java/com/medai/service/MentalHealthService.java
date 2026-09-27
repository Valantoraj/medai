package com.medai.service;

import com.medai.dto.ChatResponse;
import com.medai.model.User;
import com.medai.util.PromptTemplates;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class MentalHealthService {

    private final ChatService chatService;

    public ChatResponse chat(User user, String message, Long sessionId) {
        return chatService.processMessage(
                user, message, sessionId,
                "MENTAL_HEALTH",
                PromptTemplates.MENTAL_HEALTH_SYSTEM
        );
    }

    public Flux<String> stream(User user, String message, Long sessionId) {
        return chatService.streamMessage(
                user, message, sessionId,
                "MENTAL_HEALTH",
                PromptTemplates.MENTAL_HEALTH_SYSTEM
        );
    }
}
