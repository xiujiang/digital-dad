package com.digitaldad.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 配额流水响应
 */
@Data
@Builder
public class QuotaFlowResponse {

    private Long id;
    private String flowType;
    private Integer delta;
    private Integer balanceAfter;
    private String reason;
    private String refType;
    private String refId;
    private LocalDateTime createdAt;
}
