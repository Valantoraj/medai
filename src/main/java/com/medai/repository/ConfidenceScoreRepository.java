package com.medai.repository;

import com.medai.model.ConfidenceScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfidenceScoreRepository extends JpaRepository<ConfidenceScore, Long> {

    List<ConfidenceScore> findBySessionIdOrderByConfidencePctDesc(Long sessionId);

    Optional<ConfidenceScore> findBySessionIdAndConditionName(Long sessionId, String conditionName);

    void deleteBySessionId(Long sessionId);
}
