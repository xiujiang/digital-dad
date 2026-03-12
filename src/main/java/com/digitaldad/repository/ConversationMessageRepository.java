package com.digitaldad.project.repository;

import com.digitaldad.project.entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
