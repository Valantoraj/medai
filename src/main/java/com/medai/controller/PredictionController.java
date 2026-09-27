package com.medai.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.medai.dto.PredictionResponse;
import com.medai.model.Prediction;
import com.medai.model.User;
import com.medai.repository.PredictionRepository;
import com.medai.service.MedicalGuidanceService;
import com.medai.service.PersonalisationService;
import com.medai.service.PredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/predict")
@RequiredArgsConstructor
@Slf4j
public class PredictionController {

    private final PredictionService predictionService;
    private final MedicalGuidanceService guidanceService;
    private final PersonalisationService personalisationService;
    private final PredictionRepository predictionRepository;

    // -----------------------------------------------------------------------
    //  Tabular prediction endpoints
    // -----------------------------------------------------------------------

    @PostMapping("/heart")
    public ResponseEntity<PredictionResponse> predictHeart(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> features) {
        return tabularPredict(user, "/predict/heart", "Heart Disease", features);
    }

    @PostMapping("/stroke")
    public ResponseEntity<PredictionResponse> predictStroke(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> features) {
        return tabularPredict(user, "/predict/stroke", "Stroke", features);
    }

    @PostMapping("/diabetes")
    public ResponseEntity<PredictionResponse> predictDiabetes(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> features) {
        return tabularPredict(user, "/predict/diabetes", "Diabetes", features);
    }

    @PostMapping("/lung-tabular")
    public ResponseEntity<PredictionResponse> predictLung(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> features) {
        return tabularPredict(user, "/predict/lung-tabular", "Lung Cancer (Risk Factors)", features);
    }

    @PostMapping("/kidney-tabular")
    public ResponseEntity<PredictionResponse> predictKidney(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> features) {
        return tabularPredict(user, "/predict/kidney-tabular", "Kidney Stone", features);
    }

    @PostMapping("/liver")
    public ResponseEntity<PredictionResponse> predictLiver(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> features) {
        return tabularPredict(user, "/predict/liver", "Liver Disease", features);
    }

    // -----------------------------------------------------------------------
    //  Image prediction endpoints
    // -----------------------------------------------------------------------

    @PostMapping(value = "/image/lung", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PredictionResponse> predictLungImage(
            @AuthenticationPrincipal User user,
            @RequestParam("image") MultipartFile image) {
        return imagePredict(user, "/predict/image/lung", "Lung Cancer", "CT Scan", image);
    }

    @PostMapping(value = "/image/skin", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PredictionResponse> predictSkinImage(
            @AuthenticationPrincipal User user,
            @RequestParam("image") MultipartFile image) {
        return imagePredict(user, "/predict/image/skin", "Skin Cancer", "Dermoscopy", image);
    }

    @PostMapping(value = "/image/blood", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PredictionResponse> predictBloodImage(
            @AuthenticationPrincipal User user,
            @RequestParam("image") MultipartFile image) {
        return imagePredict(user, "/predict/image/blood", "Blood Cancer / Leukemia", "Blood Smear", image);
    }

    @PostMapping(value = "/image/kidney", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PredictionResponse> predictKidneyImage(
            @AuthenticationPrincipal User user,
            @RequestParam("image") MultipartFile image) {
        return imagePredict(user, "/predict/image/kidney", "Kidney Condition", "CT Scan", image);
    }

    @PostMapping(value = "/image/brain", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PredictionResponse> predictBrainImage(
            @AuthenticationPrincipal User user,
            @RequestParam("image") MultipartFile image) {
        return imagePredict(user, "/predict/image/brain", "Brain Tumor", "MRI", image);
    }

    // -----------------------------------------------------------------------
    //  History
    // -----------------------------------------------------------------------

    @GetMapping("/history")
    public ResponseEntity<List<Prediction>> getHistory(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(predictionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));
    }

    // -----------------------------------------------------------------------
    //  Internal helpers
    // -----------------------------------------------------------------------

    private ResponseEntity<PredictionResponse> tabularPredict(
            User user, String mlEndpoint, String diseaseName, Map<String, Object> features) {
        try {
            JsonNode mlResult = predictionService.predictTabular(mlEndpoint, features);

            double riskProb = mlResult.path("risk_probability").asDouble(0.0);
            String riskPct = mlResult.path("risk_percentage").asText("?%");
            String riskLevel = mlResult.path("risk_level").asText("MEDIUM");
            String modelUsed = mlResult.path("model_used").asText("");

            // Build patient context for personalisation
            String patientContext = Boolean.TRUE.equals(user.getPersonalisation())
                    ? personalisationService.buildPersonalisationContext(user.getId(), diseaseName)
                    : "";

            // Generate Ollama guidance
            String guidance = guidanceService.generateTabularGuidance(
                    diseaseName, riskPct, riskLevel, patientContext);

            boolean triggerHospital = guidanceService.shouldTriggerHospitalFinder(riskLevel, null);
            String specialty = triggerHospital ? guidanceService.mapToSpecialty(diseaseName, null) : null;

            // Persist prediction
            savePrediction(user, "TABULAR", diseaseName, riskProb, riskLevel,
                    null, null, guidance, modelUsed);

            // Log interaction
            personalisationService.logInteraction(user.getId(), "PREDICTION_" + diseaseName.toUpperCase()
                    .replace(" ", "_"), diseaseName + " risk assessment", riskLevel + " risk: " + riskPct);

            return ResponseEntity.ok(PredictionResponse.builder()
                    .disease(diseaseName)
                    .riskProbability(riskProb)
                    .riskPercentage(riskPct)
                    .riskLevel(riskLevel)
                    .modelUsed(modelUsed)
                    .predictionType("TABULAR")
                    .guidance(guidance)
                    .triggerHospitalFinder(triggerHospital)
                    .hospitalSpecialtyFilter(specialty)
                    .personalised(!patientContext.isBlank())
                    .interactionLogged(true)
                    .build());

        } catch (Exception e) {
            log.error("Tabular prediction error for {}: {}", diseaseName, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(PredictionResponse.builder()
                            .disease(diseaseName)
                            .error(e.getMessage())
                            .build());
        }
    }

    private ResponseEntity<PredictionResponse> imagePredict(
            User user, String mlEndpoint, String cancerType, String imageType, MultipartFile image) {
        try {
            byte[] imageBytes = image.getBytes();
            String filename = image.getOriginalFilename() != null ? image.getOriginalFilename() : "image.jpg";

            JsonNode mlResult = predictionService.predictImage(mlEndpoint, imageBytes, filename);

            String predictedClass = mlResult.path("predicted_class").asText("Unknown");
            double confidence = mlResult.path("confidence").asDouble(0.0);
            String confidencePct = mlResult.path("confidence_pct").asText("?%");
            boolean aboveThreshold = mlResult.path("above_threshold").asBoolean(false);
            String modelUsed = mlResult.path("model_used").asText("");
            String severity = mlResult.path("severity").asText("MEDIUM");

            String patientContext = Boolean.TRUE.equals(user.getPersonalisation())
                    ? personalisationService.buildPersonalisationContext(user.getId(), cancerType)
                    : "";

            String guidance = guidanceService.generateImageGuidance(
                    cancerType, predictedClass, confidencePct, imageType, patientContext);

            boolean triggerHospital = guidanceService.shouldTriggerHospitalFinder(severity, predictedClass);
            String specialty = triggerHospital ? guidanceService.mapToSpecialty(cancerType, predictedClass) : null;

            savePrediction(user, "IMAGE", cancerType, null, severity,
                    predictedClass, confidence, guidance, modelUsed);

            personalisationService.logInteraction(user.getId(), "IMAGE_" + cancerType.toUpperCase()
                    .replace(" ", "_").replace("/", "_"),
                    cancerType + " image scan", predictedClass + " (" + confidencePct + ")");

            return ResponseEntity.ok(PredictionResponse.builder()
                    .disease(cancerType)
                    .predictedClass(predictedClass)
                    .confidence(confidence)
                    .confidencePct(confidencePct)
                    .aboveThreshold(aboveThreshold)
                    .modelUsed(modelUsed)
                    .predictionType("IMAGE")
                    .guidance(guidance)
                    .triggerHospitalFinder(triggerHospital)
                    .hospitalSpecialtyFilter(specialty)
                    .personalised(!patientContext.isBlank())
                    .interactionLogged(true)
                    .build());

        } catch (Exception e) {
            log.error("Image prediction error for {}: {}", cancerType, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(PredictionResponse.builder()
                            .disease(cancerType)
                            .error(e.getMessage())
                            .build());
        }
    }

    private void savePrediction(User user, String type, String disease,
                                 Double riskProb, String riskLevel,
                                 String predictedClass, Double confidence,
                                 String guidance, String modelUsed) {
        try {
            Prediction p = Prediction.builder()
                    .user(user)
                    .predictionType(type)
                    .disease(disease)
                    .riskProbability(riskProb != null ? BigDecimal.valueOf(riskProb) : null)
                    .riskLevel(riskLevel)
                    .predictedClass(predictedClass)
                    .confidencePct(confidence != null ? BigDecimal.valueOf(confidence) : null)
                    .guidanceText(guidance)
                    .modelUsed(modelUsed)
                    .build();
            predictionRepository.save(p);
        } catch (Exception e) {
            log.warn("Could not save prediction to DB: {}", e.getMessage());
        }
    }
}
