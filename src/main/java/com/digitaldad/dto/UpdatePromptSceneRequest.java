package com.digitaldad.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新提示词场景请求
 */
@Data
public class UpdatePromptSceneRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 32)
    private String boardCode;

    private String roleType;

    @Size(max = 500)
    private String description;

    private String status;
}
