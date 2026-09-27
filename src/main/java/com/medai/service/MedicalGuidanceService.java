package com.medai.service;

import com.medai.config.OllamaConfig;
import com.medai.util.PromptTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalGuidanceService {

    private final OllamaService ollamaService;
    private final OllamaConfig ollamaConfig;

    /**
     * Generate medical guidance after a tabular ML prediction.
     */
    public String generateTabularGuidance(String diseaseName,
                                           String riskPercentage,
                                           String riskLevel,
                                           String patientContext) {
        String prompt = PromptTemplates.buildTabularGuidancePrompt(
                diseaseName, riskPercentage, riskLevel, patientContext);
        return callWithFallback(prompt);
    }

    /**
     * Generate medical guidance after an image ML prediction.
     */
    public String generateImageGuidance(String cancerType,
                                         String predictedClass,
                                         String confidencePct,
                                         String imageType,
                                         String patientContext) {
        String prompt = PromptTemplates.buildImageGuidancePrompt(
                cancerType, predictedClass, confidencePct, imageType, patientContext);
        return callWithFallback(prompt);
    }

    private String callWithFallback(String prompt) {
        try {
            return ollamaService.query(ollamaConfig.getGuidanceModel(), prompt);
        } catch (Exception e) {
            log.warn("Guidance model failed, trying fallback: {}", e.getMessage());
            try {
                return ollamaService.query(ollamaConfig.getFallbackModel(), prompt);
            } catch (Exception e2) {
                log.error("Fallback guidance model also failed: {}", e2.getMessage());
                return "Unable to generate guidance at this time. Please consult a healthcare professional.";
            }
        }
    }

    /**
     * Map risk level + class to hospital specialty filter.
     */
    public String mapToSpecialty(String disease, String predictedClass) {
        String d = (disease != null ? disease : "").toLowerCase();
        String c = (predictedClass != null ? predictedClass : "").toLowerCase();
        String combined = d + " " + c;

        if (combined.contains("heart") || combined.contains("cardio")) return "Cardiology";
        if (combined.contains("stroke") || combined.contains("neuro")) return "Neurology";
        if (combined.contains("diabetes") || combined.contains("endocrin")) return "Endocrinology";
        if (combined.contains("lung") || combined.contains("pulm")) return "Pulmonology";
        if (combined.contains("liver") || combined.contains("hepat")) return "Hepatology";
        if (combined.contains("kidney") || combined.contains("renal") || combined.contains("nephr")) return "Nephrology";
        if (combined.contains("skin") || combined.contains("dermat")) return "Dermatology";
        if (combined.contains("brain") || combined.contains("tumor") || combined.contains("glioma")) return "Neurosurgery";
        if (combined.contains("blood") || combined.contains("leukemia")) return "Haematology/Oncology";
        if (combined.contains("cancer") || combined.contains("malignant")) return "Oncology";
        return "General Medicine";
    }

    /**
     * Determine if hospital finder should be triggered based on risk level.
     */
    public boolean shouldTriggerHospitalFinder(String riskLevel, String predictedClass) {
        if (riskLevel != null && (riskLevel.equals("HIGH") || riskLevel.equals("CRITICAL"))) {
            return true;
        }
        if (predictedClass != null) {
            String c = predictedClass.toLowerCase();
            return c.contains("malignant") || c.contains("tumor") ||
                   c.contains("glioma") || c.contains("meningioma") ||
                   c.contains("pre-b") || c.contains("pro-b");
        }
        return false;
    }
}
