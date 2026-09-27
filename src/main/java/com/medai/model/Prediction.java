package com.medai.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "predictions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "prediction_type", nullable = false, length = 10)
    private String predictionType; // TABULAR or IMAGE

    @Column(nullable = false, length = 100)
    private String disease;

    @Column(name = "risk_probability", precision = 5, scale = 2)
    private BigDecimal riskProbability;

    @Column(name = "risk_level", length = 10)
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "predicted_class", length = 100)
    private String predictedClass;

    @Column(name = "confidence_pct", precision = 5, scale = 2)
    private BigDecimal confidencePct;

    @Column(name = "guidance_text", columnDefinition = "TEXT")
    private String guidanceText;

    @Column(name = "model_used", length = 200)
    private String modelUsed;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
