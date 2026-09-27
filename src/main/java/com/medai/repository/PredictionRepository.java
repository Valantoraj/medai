package com.medai.repository;

import com.medai.model.Prediction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    List<Prediction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Prediction> findByUserIdOrderByCreatedAtDesc(Long userId);
}
