package com.medai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private Long sessionId;
    private String botType;
    private String message;
    private String role; // ASSISTANT

    // Confidence score data
    private List<ConfidenceUpdate.ConditionScore> confidenceScores;
    private String topCondition;
    private Double topConfidence;
    private boolean thresholdReached;

    // Hospital finder trigger
    private boolean triggerHospitalFinder;
    private String hospitalSpecialtyFilter;

    // Personalisation metadata
    private boolean personalised;

    // Message count in this session
    private Long messageCount;
}
