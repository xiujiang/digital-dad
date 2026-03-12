package com.digitaldad.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 主持人列表项响应
 */
@Data
@Builder
public class AdminHostListItemResponse {

    private Long id;
    private String name;
    private String phone;
    private String status;
    private Integer quotaRemaining;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
