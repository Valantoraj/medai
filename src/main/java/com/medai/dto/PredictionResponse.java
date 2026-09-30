package com.medai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResponse {

    // ML Prediction part
    private String disease;
    private Double riskProbability;
    private String riskPercentage;
    private String riskLevel;        // LOW / MEDIUM / HIGH / CRITICAL (tabular)
    private String predictedClass;   // null for tabular, populated for image
    private Double confidence;       // null for tabular, populated for image
    private String confidencePct;
    private Boolean aboveThreshold;
    private String modelUsed;
    private String predictionType;   // TABULAR or IMAGE

    // Ollama Guidance part
    private String guidance;

    // Hospital finder trigger
    private boolean triggerHospitalFinder;
    private String hospitalSpecialtyFilter;

    // Personalisation metadata
    private boolean personalised;
    private boolean interactionLogged;

    // Error field (null on success)
    private String error;

    // Annotated scan image (base64-encoded JPEG, image predictions only)
    private String annotatedImageB64;
}
