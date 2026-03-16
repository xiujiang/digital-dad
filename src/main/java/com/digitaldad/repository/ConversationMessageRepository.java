package com.digitaldad.repository;

import com.digitaldad.entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 对话消息 Repository
 */
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    List<ConversationMessage> findBySessionIdOrderBySequenceNoAsc(Long sessionId);

    List<ConversationMessage> findBySessionIdAndIsSubmittedFalse(Long sessionId);

    Optional<ConversationMessage> findBySessionIdAndId(Long sessionId, Long id);

    int countBySessionIdAndBatchNoGreaterThan(Long sessionId, int batchNo);

    /** 今日新增且有音频 URL 的消息数（用于存储对象数近似统计） */
    @Query("SELECT COUNT(m) FROM ConversationMessage m WHERE m.createdAt >= :start AND m.createdAt < :end AND m.audioUrl IS NOT NULL")
    long countByCreatedAtBetweenAndAudioUrlNotNull(
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
