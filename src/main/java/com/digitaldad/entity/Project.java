package com.digitaldad.entity;

import com.digitaldad.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目主表
 */
@Getter
@Setter
@Entity
@Table(name = "project")
@SQLDelete(sql = "UPDATE project SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_no", nullable = false, unique = true, length = 32)
    private String projectNo;

    @Column(name = "host_user_id", nullable = false)
    private Long hostUserId;

    @Column(name = "groom_name", length = 50)
    private String groomName;

    @Column(name = "bride_name", length = 50)
    private String brideName;

    @Column(name = "wedding_date")
    private LocalDate weddingDate;

    @Column(name = "theme", length = 200)
    private String theme;

    /** 联系方式（与项目绑定） */
    @Column(name = "contact_info", length = 100)
    private String contactInfo;

    @Column(name = "share_token", nullable = false, unique = true, length = 64)
    private String shareToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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
