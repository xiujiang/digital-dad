package com.digitaldad.repository;

import com.digitaldad.entity.InterviewSession;
import com.digitaldad.enums.SessionStatus;
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

    /** 按参与者 + 板块查会话（唯一约束 uk_participant_board，用于「进入某板块」时先查是否已有会话） */
    Optional<InterviewSession> findByParticipantIdAndCurrentProjectBoardId(
            Long participantId, Long currentProjectBoardId);

    /** 按参与者 + 板块 + 状态查会话（用于「进入某板块」时恢复或创建） */
    Optional<InterviewSession> findByParticipantIdAndCurrentProjectBoardIdAndStatusIn(
            Long participantId, Long currentProjectBoardId, List<SessionStatus> statuses);

    /** 某参与者在某项目下的所有会话（用于 my-status 按板块聚合进度） */
    List<InterviewSession> findByParticipantIdAndProjectIdOrderByCreatedAtAsc(
            Long participantId, Long projectId);

    List<InterviewSession> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    boolean existsByParticipantIdAndStatus(Long participantId, SessionStatus status);

    /** 今日活跃会话数：last_active_at 在今日，或今日有消息的会话去重 */
    @Query("SELECT COUNT(DISTINCT s.id) FROM InterviewSession s WHERE " +
            "(s.lastActiveAt >= :start AND s.lastActiveAt < :end) " +
            "OR EXISTS (SELECT 1 FROM ConversationMessage m WHERE m.sessionId = s.id AND m.createdAt >= :start AND m.createdAt < :end)")
    long countActiveSessionsToday(
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
