package com.digitaldad.project.entity;

import com.digitaldad.project.enums.SummaryItemType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 小结条目
 */
@Getter
@Setter
@Entity
@Table(name = "summary_item")
public class SummaryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "summary_id", nullable = false)
    private Long summaryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private SummaryItemType itemType = SummaryItemType.FACT;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Column(name = "item_order", nullable = false)
    private Integer itemOrder = 0;

    @Column(name = "is_selected", nullable = false)
    private Boolean isSelected = true;

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
