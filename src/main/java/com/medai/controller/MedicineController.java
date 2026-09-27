package com.medai.controller;

import com.medai.dto.ChatRequest;
import com.medai.dto.ChatResponse;
import com.medai.model.User;
import com.medai.service.MedicineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/medicine")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    /**
     * Conversational mode — maintains full session history.
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(medicineService.chat(user, request.getMessage(), request.getSessionId()));
    }

    /**
     * Single-ask mode — no session, one-shot answer.
     */
    @PostMapping("/query")
    public ResponseEntity<Map<String, String>> query(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        String question = body.getOrDefault("question", "");
        if (question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question is required"));
        }
        String answer = medicineService.singleQuery(question);
        return ResponseEntity.ok(Map.of("answer", answer));
    }
}
