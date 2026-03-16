package com.digitaldad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 场景绑定提示词请求
 */
@Data
public class AddSceneItemRequest {

    @NotBlank(message = "提示词编码不能为空")
    @Size(max = 64)
    private String promptCode;

    private Integer displayOrder = 0;

    private String usageMode = "APPEND";
}
