package com.digitaldad.entity;

import com.digitaldad.enums.ContactVisible;
import com.digitaldad.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 用户主表
 */
@Getter
@Setter
@Entity
@Table(name = "`user`")
@SQLDelete(sql = "UPDATE `user` SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ENABLED;

    @Column(name = "phone", length = 11)
    private String phone;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /** 密码哈希（BCrypt），可为空；设置后支持手机号+密码登录 */
    @Column(name = "password_hash", length = 128)
    private String passwordHash;

    /** 最后修改密码时间（用于定期修改密码策略） */
    @Column(name = "last_password_changed_at")
    private LocalDateTime lastPasswordChangedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_visible", length = 20)
    private ContactVisible contactVisible = ContactVisible.PUBLIC;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

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
