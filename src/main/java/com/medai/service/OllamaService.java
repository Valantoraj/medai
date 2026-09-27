package com.medai.service;

import com.medai.config.OllamaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaService {

    private final OllamaChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final OllamaConfig ollamaConfig;

    /**
     * Send a chat request to Ollama with a specific model, system prompt, and message history.
     * Returns the full response string.
     * think:false is passed via additionalProperties to disable reasoning for thinking models.
     */
    public String chat(String modelName, String systemPrompt, List<Message> history, String userMessage) {
        try {
            List<Message> messages = buildMessages(systemPrompt, history, userMessage);
            OllamaOptions options = buildOptions(modelName);
            Prompt prompt = new Prompt(messages, options);
            ChatResponse response = chatModel.call(prompt);
            return extractText(response);
        } catch (Exception e) {
            log.error("Error calling Ollama model {}: {}", modelName, e.getMessage());
            if (!modelName.equals(ollamaConfig.getFallbackModel())) {
                log.info("Falling back to model: {}", ollamaConfig.getFallbackModel());
                return chat(ollamaConfig.getFallbackModel(), systemPrompt, history, userMessage);
            }
            throw new RuntimeException("Ollama service unavailable: " + e.getMessage(), e);
        }
    }

    /**
     * Stream a chat response token-by-token via Flux (for SSE).
     */
    public Flux<String> streamChat(String modelName, String systemPrompt, List<Message> history, String userMessage) {
        try {
            List<Message> messages = buildMessages(systemPrompt, history, userMessage);
            OllamaOptions options = buildOptions(modelName);
            Prompt prompt = new Prompt(messages, options);
            return chatModel.stream(prompt)
                    .map(response -> {
                        String text = extractText(response);
                        return text != null ? text : "";
                    })
                    .onErrorResume(e -> {
                        log.error("Streaming error for model {}: {}", modelName, e.getMessage());
                        return Flux.just("[Error: " + e.getMessage() + "]");
                    });
        } catch (Exception e) {
            log.error("Failed to start stream for model {}: {}", modelName, e.getMessage());
            return Flux.just("[Error: Ollama service unavailable]");
        }
    }

    /**
     * Generate an embedding vector for a text string using nomic-embed-text.
     */
    public float[] generateEmbedding(String text) {
        try {
            var response = embeddingModel.embedForResponse(List.of(text));
            return response.getResults().get(0).getOutput();
        } catch (Exception e) {
            log.error("Error generating embedding: {}", e.getMessage());
            return new float[768];
        }
    }

    /**
     * Simple single-turn query — no history, just a prompt.
     */
    public String query(String modelName, String prompt) {
        return chat(modelName, "", List.of(), prompt);
    }

    /**
     * Convert stored chat message role strings to Spring AI Message objects.
     */
    public Message toMessage(String role, String content) {
        return switch (role.toUpperCase()) {
            case "USER" -> UserMessage.builder().text(content).build();
            case "ASSISTANT" -> new AssistantMessage(content);
            case "SYSTEM" -> new SystemMessage(content);
            default -> UserMessage.builder().text(content).build();
        };
    }

    // ── Private helpers ────────────────────────────────────────

    private OllamaOptions buildOptions(String modelName) {
        return OllamaOptions.builder()
                .model(modelName)
                .temperature(0.7)
                .numPredict(1024)
                .build();
    }

    private List<Message> buildMessages(String systemPrompt, List<Message> history, String userMessage) {
        List<Message> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new SystemMessage(systemPrompt));
        }
        if (history != null) {
            messages.addAll(history);
        }
        messages.add(UserMessage.builder().text(userMessage).build());
        return messages;
    }

    /**
     * Extract text from ChatResponse — handles thinking models where getText() may return null
     * when the model returns only a thinking block with no final answer text.
     */
    private String extractText(ChatResponse response) {
        if (response == null || response.getResult() == null) return "";
        var output = response.getResult().getOutput();
        if (output == null) return "";

        String text = output.getText();
        if (text != null && !text.isBlank()) return text;

        return "";
    }
}
