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
public class ConfidenceUpdate {

    private Long sessionId;
    private List<ConditionScore> scores;
    private String topCondition;
    private double topConfidence;
    private boolean thresholdReached;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConditionScore {
        private String condition;
        private double confidence;
        private String reasoning;
    }
}
