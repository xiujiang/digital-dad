package com.digitaldad.prompt.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 场景绑定模板请求
 */
@Data
public class AddSceneItemRequest {

    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    private Integer displayOrder = 0;

    private String usageMode = "APPEND";
}
