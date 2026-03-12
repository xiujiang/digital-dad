package com.digitaldad.project.entity;

import com.digitaldad.project.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 采访会话
 */
@Getter
@Setter
@Entity
@Table(name = "interview_session")
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "participant_id", nullable = false)
    private Long participantId;

    @Column(name = "current_project_board_id")
    private Long currentProjectBoardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(name = "round_count", nullable = false)
    private Integer roundCount = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
