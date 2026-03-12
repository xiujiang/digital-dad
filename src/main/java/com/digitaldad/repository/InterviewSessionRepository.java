package com.digitaldad.project.repository;

import com.digitaldad.project.entity.InterviewSession;
import com.digitaldad.project.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
