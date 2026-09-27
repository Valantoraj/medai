package com.medai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "Message content is required")
    private String message;

    // Optional — if null, a new session is created
    private Long sessionId;

    @NotBlank(message = "Bot type is required")
    private String botType; // MENTAL_HEALTH, COMMON_DISEASE, COMPLEX_DISEASE, MEDICINE

    private boolean stream = false;
}
