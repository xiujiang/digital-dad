package com.digitaldad.entity;

import com.digitaldad.enums.PromptRoleType;
import com.digitaldad.enums.PromptSceneScope;
import com.digitaldad.enums.PromptStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 提示词使用场景
 */
@Getter
@Setter
@Entity
@Table(name = "prompt_scene")
public class PromptScene {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 32)
    private PromptSceneScope scope;

    @Column(name = "board_code", length = 32)
    private String boardCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", length = 20)
    private PromptRoleType roleType;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PromptStatus status = PromptStatus.ENABLED;

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
