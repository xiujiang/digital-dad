package com.digitaldad.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 会话-板块轮数（按板块统计每会话的对话轮数，用于轮数上限）
 */
@Getter
@Setter
@Entity
@Table(name = "session_board_rounds", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"session_id", "project_board_id"})
})
public class SessionBoardRounds {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "project_board_id", nullable = false)
    private Long projectBoardId;

    @Column(name = "round_count", nullable = false)
    private Integer roundCount = 0;

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
