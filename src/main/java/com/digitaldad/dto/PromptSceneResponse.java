package com.digitaldad.prompt.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提示词场景响应
 */
@Data
@Builder
public class PromptSceneResponse {

    private Long id;
    private String code;
    private String name;
    private String scope;
    private String boardCode;
    private String roleType;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 绑定的模板项列表 */
    private List<PromptSceneItemResponse> items;
}
