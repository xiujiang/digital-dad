package com.digitaldad.prompt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建提示词模板请求
 */
@Data
public class CreatePromptTemplateRequest {

    @NotBlank(message = "编码不能为空")
    @Size(max = 64)
    private String code;

    @NotBlank(message = "名称不能为空")
    @Size(max = 100)
    private String name;

    private String contentType = "TEXT";

    @Size(max = 500)
    private String description;

    private String status = "ENABLED";
}
