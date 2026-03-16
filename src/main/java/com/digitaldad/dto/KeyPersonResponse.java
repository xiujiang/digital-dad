package com.digitaldad.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关键人物响应
 */
@Data
@Builder
public class KeyPersonResponse {

    private Long id;
    private Long userId;
    private Long sessionId;
    private String name;
    private String roleLabel;
    private LocalDateTime createdAt;
}
