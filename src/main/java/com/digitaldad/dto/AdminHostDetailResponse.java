package com.digitaldad.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 主持人详情响应
 */
@Data
@Builder
public class AdminHostDetailResponse {

    private Long id;
    private String phone;
    private String name;
    private String status;
    private Integer quotaRemaining;
    private Integer quotaTotalUsed;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
