package com.medai.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_interactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String feature;

    @Column(name = "input_summary", columnDefinition = "TEXT")
    private String inputSummary;

    @Column(name = "output_summary", columnDefinition = "TEXT")
    private String outputSummary;

    // Stored as TEXT in Java — inserted as vector via native SQL in repository
    // Avoids Hibernate bytea/vector type conflict with pgvector
    @Column(name = "embedding", columnDefinition = "vector(768)", insertable = false, updatable = false)
    private String embeddingPlaceholder;

    // Transient holder used during save — see UserInteractionRepository.saveWithEmbedding()
    @Transient
    private float[] embedding;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
