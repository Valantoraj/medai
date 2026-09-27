package com.medai.controller;

import com.medai.dto.ChatRequest;
import com.medai.dto.ChatResponse;
import com.medai.dto.ConfidenceUpdate;
import com.medai.model.ChatMessage;
import com.medai.model.ChatSession;
import com.medai.model.ConfidenceScore;
import com.medai.model.User;
import com.medai.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final MentalHealthService mentalHealthService;
    private final CommonDiseaseService commonDiseaseService;
    private final ComplexDiseaseService complexDiseaseService;
    private final ChatService chatService;
    private final ConfidenceEngine confidenceEngine;

    // -----------------------------------------------------------------------
    //  Mental Health Chatbot
    // -----------------------------------------------------------------------
    @PostMapping("/mental-health")
    public ResponseEntity<ChatResponse> mentalHealth(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChatRequest request) {
        ChatResponse response = mentalHealthService.chat(user, request.getMessage(), request.getSessionId());
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/mental-health/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter mentalHealthStream(
            @AuthenticationPrincipal User user,
            @RequestParam String message,
            @RequestParam(required = false) Long sessionId) {
        return buildSseEmitter(mentalHealthService.stream(user, message, sessionId));
    }

    // -----------------------------------------------------------------------
    //  Common Disease Diagnoser
    // -----------------------------------------------------------------------
    @PostMapping("/common-disease")
    public ResponseEntity<ChatResponse> commonDisease(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChatRequest request) {
        ChatResponse response = commonDiseaseService.chat(user, request.getMessage(), request.getSessionId());
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/common-disease/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter commonDiseaseStream(
            @AuthenticationPrincipal User user,
            @RequestParam String message,
            @RequestParam(required = false) Long sessionId) {
        return buildSseEmitter(commonDiseaseService.stream(user, message, sessionId));
    }

    // -----------------------------------------------------------------------
    //  Complex Disease Diagnoser
    // -----------------------------------------------------------------------
    @PostMapping("/complex-disease")
    public ResponseEntity<ChatResponse> complexDisease(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChatRequest request) {
        ChatResponse response = complexDiseaseService.chat(user, request.getMessage(), request.getSessionId());
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/complex-disease/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter complexDiseaseStream(
            @AuthenticationPrincipal User user,
            @RequestParam String message,
            @RequestParam(required = false) Long sessionId) {
        return buildSseEmitter(complexDiseaseService.stream(user, message, sessionId));
    }

    // -----------------------------------------------------------------------
    //  Session & History
    // -----------------------------------------------------------------------
    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSession>> getSessions(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.getUserSessions(user.getId()));
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<List<ChatMessage>> getSessionMessages(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(chatService.getSessionMessages(id));
    }

    @GetMapping("/confidence/{sessionId}")
    public ResponseEntity<List<ConfidenceScore>> getConfidenceScores(
            @AuthenticationPrincipal User user,
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(confidenceEngine.getScoresForSession(sessionId));
    }

    // -----------------------------------------------------------------------
    //  SSE helper
    // -----------------------------------------------------------------------
    private SseEmitter buildSseEmitter(Flux<String> flux) {
        SseEmitter emitter = new SseEmitter(120_000L); // 2 min timeout
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            try {
                flux.toIterable().forEach(token -> {
                    try {
                        emitter.send(SseEmitter.event().data(token));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                });
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            } finally {
                executor.shutdown();
            }
        });

        return emitter;
    }
}
