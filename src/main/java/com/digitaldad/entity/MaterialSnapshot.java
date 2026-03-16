package com.digitaldad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 素材快照（确认后冻结，供交付物生成）
 */
@Getter
@Setter
@Entity
@Table(name = "material_snapshot")
public class MaterialSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "participant_id", nullable = false)
    private Long participantId;

    @Column(name = "project_board_id", nullable = false)
    private Long projectBoardId;

    @Column(name = "summary_id", nullable = false)
    private Long summaryId;

    @Column(name = "snapshot_payload", nullable = false, columnDefinition = "TEXT")
    private String snapshotPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
