package com.digitaldad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 语音转写使用记录
 */
@Getter
@Setter
@Entity
@Table(name = "speech_transcription_usage")
public class SpeechTranscriptionUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds = 0;

    @Column(name = "deducted_seconds", nullable = false)
    private Integer deductedSeconds = 0;

    @Column(name = "remaining_after", nullable = false)
    private Integer remainingAfter = 0;

    @Column(name = "connect_id", length = 64)
    private String connectId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
