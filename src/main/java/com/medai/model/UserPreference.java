package com.medai.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "known_conditions", columnDefinition = "TEXT[]")
    private String[] knownConditions;

    @Column(name = "medication_hist", columnDefinition = "TEXT[]")
    private String[] medicationHist;

    @Column(columnDefinition = "TEXT[]")
    private String[] allergies;

    @Column(name = "language_style", length = 20)
    @Builder.Default
    private String languageStyle = "simple";

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
