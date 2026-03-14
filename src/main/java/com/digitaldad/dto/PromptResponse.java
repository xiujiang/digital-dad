package com.digitaldad.prompt.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提示词响应（单行：当前生效或指定版本）
 */
@Data
@Builder
public class PromptResponse {

    private Long id;
    private String code;
    private String name;
    private String contentType;
    private String description;
    private String status;
    private Integer versionNo;
    private String content;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
}
