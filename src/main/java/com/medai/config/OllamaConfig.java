package com.medai.config;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;

@Configuration
@Getter
public class OllamaConfig {

    @Value("${app.ollama.models.mental-health}")
    private String mentalHealthModel;

    @Value("${app.ollama.models.common-disease}")
    private String commonDiseaseModel;

    @Value("${app.ollama.models.complex-disease}")
    private String complexDiseaseModel;

    @Value("${app.ollama.models.medicine}")
    private String medicineModel;

    @Value("${app.ollama.models.embedding}")
    private String embeddingModel;

    @Value("${app.ollama.models.fallback}")
    private String fallbackModel;

    @Value("${app.ollama.models.guidance}")
    private String guidanceModel;

    @Value("${app.confidence.threshold-common}")
    private int thresholdCommon;

    @Value("${app.confidence.threshold-complex}")
    private int thresholdComplex;

    @Value("${app.confidence.threshold-mental}")
    private int thresholdMental;
}
