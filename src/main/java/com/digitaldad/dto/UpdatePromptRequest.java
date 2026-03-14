package com.digitaldad.prompt.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新提示词请求（按 id 更新当前行）
 */
@Data
public class UpdatePromptRequest {

    @Size(max = 100)
    private String name;

    private String contentType;

    @Size(max = 500)
    private String description;

    private String status;

    private String content;
}
