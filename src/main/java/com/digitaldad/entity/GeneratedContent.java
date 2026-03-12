package com.digitaldad.project.entity;

import com.digitaldad.project.enums.ContentStatus;
import com.digitaldad.project.enums.ContentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 生成物表（与项目绑定）
 */
@Getter
@Setter
@Entity
@Table(name = "generated_content")
public class GeneratedContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "participant_id")
    private Long participantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private ContentType contentType;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo = 1;

    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ContentStatus status = ContentStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "snapshot_version_at")
    private LocalDateTime snapshotVersionAt;

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
