package com.digitaldad.project.entity;

import com.digitaldad.project.enums.SummaryStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 板块小结
 */
@Getter
@Setter
@Entity
@Table(name = "board_summary")
public class BoardSummary {

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

    @Column(name = "version_no", nullable = false)
    private Integer versionNo = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SummaryStatus status = SummaryStatus.DRAFT;

    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "content_json", columnDefinition = "TEXT")
    private String contentJson;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

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
