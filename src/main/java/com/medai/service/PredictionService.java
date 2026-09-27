package com.medai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionService {

    @Qualifier("mlServiceWebClient")
    private final WebClient mlClient;
    private final ObjectMapper objectMapper;

    /**
     * Call Python ML service for tabular prediction.
     */
    public JsonNode predictTabular(String endpoint, Map<String, Object> features) {
        try {
            String json = mlClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(features)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(30))
                    .block();

            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("ML service tabular prediction failed for {}: {}", endpoint, e.getMessage());
            throw new RuntimeException("ML service unavailable: " + e.getMessage(), e);
        }
    }

    /**
     * Call Python ML service for image prediction (multipart upload).
     */
    public JsonNode predictImage(String endpoint, byte[] imageBytes, String filename) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("image", new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            });

            String json = mlClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(60))
                    .block();

            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("ML service image prediction failed for {}: {}", endpoint, e.getMessage());
            throw new RuntimeException("ML service unavailable: " + e.getMessage(), e);
        }
    }

    /**
     * Check if ML service is healthy.
     */
    public boolean isHealthy() {
        try {
            String res = mlClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(5))
                    .block();
            return res != null && res.contains("ok");
        } catch (Exception e) {
            return false;
        }
    }
}
