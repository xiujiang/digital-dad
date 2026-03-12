package com.digitaldad.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 关键人物
 */
@Getter
@Setter
@Entity
@Table(name = "key_person")
public class KeyPerson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "participant_id", nullable = false)
    private Long participantId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "role_label", length = 50)
    private String roleLabel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
