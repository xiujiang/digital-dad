package com.digitaldad.board.entity;

import com.digitaldad.board.enums.BoardMetaStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 板块元数据（平台级配置）
 */
@Getter
@Setter
@Entity
@Table(name = "board_meta")
public class BoardMeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 32)
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "description", length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BoardMetaStatus status = BoardMetaStatus.ENABLED;

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
