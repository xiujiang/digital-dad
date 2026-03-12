package com.digitaldad.prompt.entity;

import com.digitaldad.prompt.enums.PromptContentType;
import com.digitaldad.prompt.enums.PromptStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 提示词模板（原子单位）
 */
@Getter
@Setter
@Entity
@Table(name = "prompt_template")
public class PromptTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 32)
    private PromptContentType contentType = PromptContentType.TEXT;

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
