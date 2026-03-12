package com.digitaldad.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 微信用户扩展表
 */
@Getter
@Setter
@Entity
@Table(name = "user_wechat")
public class UserWechat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "openid", nullable = false, length = 64)
    private String openid;

    @Column(name = "unionid", length = 64)
    private String unionid;

    @Column(name = "app_type", nullable = false, length = 20)
    private String appType;

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "gender")
    private Integer gender;

    @Column(name = "country", length = 50)
    private String country;

    @Column(name = "province", length = 50)
    private String province;

    @Column(name = "city", length = 50)
    private String city;

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
