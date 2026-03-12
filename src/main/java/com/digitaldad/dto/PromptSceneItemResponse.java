package com.digitaldad.prompt.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 场景-模板项响应
 */
@Data
@Builder
public class PromptSceneItemResponse {

    private Long id;
    private Long sceneId;
    private Long templateId;
    private String templateCode;
    private String templateName;
    private Integer displayOrder;
    private String usageMode;
}
