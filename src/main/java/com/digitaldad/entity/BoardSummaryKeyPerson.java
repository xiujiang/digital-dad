package com.digitaldad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 小结与关键人物绑定（多对多）
 */
@Getter
@Setter
@Entity
@Table(name = "board_summary_key_person",
        uniqueConstraints = @UniqueConstraint(columnNames = {"summary_id", "key_person_id"}))
public class BoardSummaryKeyPerson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "summary_id", nullable = false)
    private Long summaryId;

    @Column(name = "key_person_id", nullable = false)
    private Long keyPersonId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
