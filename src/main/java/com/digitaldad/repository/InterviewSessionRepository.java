package com.digitaldad.project.repository;

import com.digitaldad.project.entity.InterviewSession;
import com.digitaldad.project.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 采访会话 Repository
 */
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    Optional<InterviewSession> findByParticipantIdAndStatusIn(
            Long participantId, List<SessionStatus> statuses);

    List<InterviewSession> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    boolean existsByParticipantIdAndStatus(Long participantId, SessionStatus status);

    /** 今日活跃会话数：last_active_at 在今日，或今日有消息的会话去重 */
    @Query("SELECT COUNT(DISTINCT s.id) FROM InterviewSession s WHERE " +
            "(s.lastActiveAt >= :start AND s.lastActiveAt < :end) " +
            "OR EXISTS (SELECT 1 FROM ConversationMessage m WHERE m.sessionId = s.id AND m.createdAt >= :start AND m.createdAt < :end)")
    long countActiveSessionsToday(
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
