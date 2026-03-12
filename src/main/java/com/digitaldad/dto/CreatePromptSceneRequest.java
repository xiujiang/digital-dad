package com.digitaldad.prompt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建提示词场景请求
 */
@Data
public class CreatePromptSceneRequest {

    @NotBlank(message = "场景编码不能为空")
    @Size(max = 64)
    private String code;

    @NotBlank(message = "名称不能为空")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "适用范围不能为空")
    private String scope;

    @Size(max = 32)
    private String boardCode;

    private String roleType;

    @Size(max = 500)
    private String description;

    private String status = "ENABLED";
}
