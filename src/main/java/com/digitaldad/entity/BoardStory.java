package com.digitaldad.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 故事/时光
 */
@Getter
@Setter
@Entity
@Table(name = "board_story")
public class BoardStory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "participant_id", nullable = false)
    private Long participantId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "project_board_id", nullable = false)
    private Long projectBoardId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
