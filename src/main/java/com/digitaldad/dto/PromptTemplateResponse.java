package com.digitaldad.prompt.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提示词模板响应
 */
@Data
@Builder
public class PromptTemplateResponse {

    private Long id;
    private String code;
    private String name;
    private String contentType;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 当前生效版本号 */
    private Integer activeVersionNo;
}
