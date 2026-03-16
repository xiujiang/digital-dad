package com.digitaldad.entity;

import com.digitaldad.enums.FlowType;
import com.digitaldad.enums.QuotaType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 配额流水表
 */
@Getter
@Setter
@Entity
@Table(name = "quota_flow")
public class QuotaFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "quota_type", nullable = false, length = 32)
    private QuotaType quotaType;

    @Enumerated(EnumType.STRING)
    @Column(name = "flow_type", nullable = false, length = 20)
    private FlowType flowType;

    @Column(name = "delta", nullable = false)
    private Integer delta;

    @Column(name = "balance_after")
    private Integer balanceAfter;

    @Column(name = "reason", length = 200)
    private String reason;

    @Column(name = "ref_type", length = 50)
    private String refType;

    @Column(name = "ref_id", length = 64)
    private String refId;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
