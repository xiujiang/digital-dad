package com.digitaldad.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 场景-提示词项响应
 */
@Data
@Builder
public class PromptSceneItemResponse {

    private Long id;
    private Long sceneId;
    private String promptCode;
    private String promptName;
    private Integer displayOrder;
    private String usageMode;
}
