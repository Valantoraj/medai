package com.medai.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "confidence_scores",
       uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "condition_name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfidenceScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Column(name = "condition_name", nullable = false, length = 100)
    private String conditionName;

    @Column(name = "confidence_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal confidencePct;

    @Column(columnDefinition = "TEXT")
    private String reasoning;

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
