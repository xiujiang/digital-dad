package com.digitaldad.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提示词版本响应
 */
@Data
@Builder
public class PromptVersionResponse {

    private Long id;
    private String code;
    private Integer versionNo;
    private String content;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private Long createdBy;
}
