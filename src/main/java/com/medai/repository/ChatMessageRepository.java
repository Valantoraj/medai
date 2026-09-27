package com.medai.repository;

import com.medai.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByTimestampAsc(Long sessionId);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.session.id = :sessionId ORDER BY cm.timestamp DESC")
    List<ChatMessage> findLastNMessagesBySession(Long sessionId, Pageable pageable);

    @Query("""
        SELECT cm FROM ChatMessage cm
        WHERE cm.session.user.id = :userId
          AND cm.session.botType = :botType
        ORDER BY cm.timestamp DESC
        """)
    List<ChatMessage> findLastNMessagesByUserAndBotType(Long userId, String botType, Pageable pageable);

    long countBySessionId(Long sessionId);
}
