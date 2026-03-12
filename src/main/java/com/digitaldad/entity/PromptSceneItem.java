package com.digitaldad.prompt.entity;

import com.digitaldad.prompt.enums.PromptUsageMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 场景与模板绑定（某场景用哪些模板、顺序）
 */
@Getter
@Setter
@Entity
@Table(name = "prompt_scene_item")
public class PromptSceneItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_id", nullable = false)
    private Long sceneId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_mode", nullable = false, length = 20)
    private PromptUsageMode usageMode = PromptUsageMode.APPEND;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
