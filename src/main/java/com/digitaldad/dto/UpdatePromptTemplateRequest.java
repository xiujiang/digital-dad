package com.digitaldad.prompt.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新提示词模板请求
 */
@Data
public class UpdatePromptTemplateRequest {

    @Size(max = 100)
    private String name;

    private String contentType;

    @Size(max = 500)
    private String description;

    private String status;
}
