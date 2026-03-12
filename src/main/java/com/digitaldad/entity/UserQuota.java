package com.digitaldad.user.entity;

import com.digitaldad.user.enums.QuotaType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 配额表
 */
@Getter
@Setter
@Entity
@Table(name = "user_quota")
public class UserQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "quota_type", nullable = false, length = 20)
    private QuotaType quotaType;

    @Column(name = "remaining", nullable = false)
    private Integer remaining = 0;

    @Column(name = "total_used", nullable = false)
    private Integer totalUsed = 0;

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
