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
     */
    public String chat(String modelName, String systemPrompt, List<Message> history, String userMessage) {
        try {
            List<Message> messages = buildMessages(systemPrompt, history, userMessage);
            OllamaOptions options = OllamaOptions.builder()
                    .model(modelName)
                    .temperature(0.7)
                    .numPredict(1024)
                    .build();
            Prompt prompt = new Prompt(messages, options);
            ChatResponse response = chatModel.call(prompt);
            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("Error calling Ollama model {}: {}", modelName, e.getMessage());
            // Try fallback model
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
            OllamaOptions options = OllamaOptions.builder()
                    .model(modelName)
                    .temperature(0.7)
                    .numPredict(1024)
                    .build();
            Prompt prompt = new Prompt(messages, options);
            return chatModel.stream(prompt)
                    .map(response -> {
                        String text = response.getResult().getOutput().getText();
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
            // Spring AI 1.0.0: getOutput() returns float[]
            float[] output = response.getResults().get(0).getOutput();
            return output;
        } catch (Exception e) {
            log.error("Error generating embedding: {}", e.getMessage());
            return new float[768]; // zero vector fallback
        }
    }

    /**
     * Simple single-turn query — no history, just a prompt.
     */
    public String query(String modelName, String prompt) {
        return chat(modelName, "", List.of(), prompt);
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
}
