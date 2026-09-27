package com.medai.repository;

import com.medai.model.UserInteraction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
    List<UserInteraction> findSimilarInteractions(Long userId, String embedding, int limit);
}
