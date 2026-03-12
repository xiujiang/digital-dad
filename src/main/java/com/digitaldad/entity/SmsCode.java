package com.digitaldad.user.entity;

import com.digitaldad.user.enums.SmsScene;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 验证码记录表
 */
@Getter
@Setter
@Entity
@Table(name = "sms_code")
public class SmsCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone", nullable = false, length = 11)
    private String phone;

    @Column(name = "code", nullable = false, length = 6)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "scene", nullable = false, length = 20)
    private SmsScene scene;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
