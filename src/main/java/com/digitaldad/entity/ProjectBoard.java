package com.digitaldad.board.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 项目板块关联（项目下有哪些栏目）
 */
@Getter
@Setter
@Entity
@Table(name = "project_board")
public class ProjectBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "board_meta_id", nullable = false)
    private Long boardMetaId;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
