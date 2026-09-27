package com.medai.repository;

import com.medai.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByUserIdOrderByStartedAtDesc(Long userId);

    List<ChatSession> findByUserIdAndBotTypeOrderByStartedAtDesc(Long userId, String botType);

    Optional<ChatSession> findTopByUserIdAndBotTypeAndIsActiveTrueOrderByStartedAtDesc(Long userId, String botType);

    @Query("SELECT cs FROM ChatSession cs WHERE cs.user.id = :userId AND cs.botType = :botType ORDER BY cs.startedAt DESC")
    List<ChatSession> findRecentSessionsByUserAndType(Long userId, String botType);
}
