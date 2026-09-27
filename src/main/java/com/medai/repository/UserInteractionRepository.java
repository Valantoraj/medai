package com.medai.repository;

import com.medai.model.UserInteraction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {

    List<UserInteraction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<UserInteraction> findByUserIdAndFeatureOrderByCreatedAtDesc(Long userId, String feature);

    // Cosine similarity search using pgvector operator
    @Query(value = """
        SELECT * FROM user_interactions
        WHERE user_id = :userId
          AND embedding IS NOT NULL
        ORDER BY embedding <=> CAST(:embedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<UserInteraction> findSimilarInteractions(
            @Param("userId") Long userId,
            @Param("embedding") String embedding,
            @Param("limit") int limit);

    /**
     * Native insert that properly casts the float array string to pgvector type.
     * Called instead of save() when we have an embedding to store.
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO user_interactions
            (user_id, feature, input_summary, output_summary, embedding, created_at)
        VALUES
            (:userId, :feature, :inputSummary, :outputSummary,
             CAST(:embedding AS vector), :createdAt)
        """, nativeQuery = true)
    void insertWithEmbedding(
            @Param("userId") Long userId,
            @Param("feature") String feature,
            @Param("inputSummary") String inputSummary,
            @Param("outputSummary") String outputSummary,
            @Param("embedding") String embedding,
            @Param("createdAt") LocalDateTime createdAt);

    /**
     * Native insert without embedding (when embedding generation fails).
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO user_interactions
            (user_id, feature, input_summary, output_summary, created_at)
        VALUES
            (:userId, :feature, :inputSummary, :outputSummary, :createdAt)
        """, nativeQuery = true)
    void insertWithoutEmbedding(
            @Param("userId") Long userId,
            @Param("feature") String feature,
            @Param("inputSummary") String inputSummary,
            @Param("outputSummary") String outputSummary,
            @Param("createdAt") LocalDateTime createdAt);
}
