package com.digitaldad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建场景并同时创建并绑定一条提示词（首版）的请求
 * <p>包含场景字段 + 首条提示词字段 + 该条在场景中的顺序与用法。</p>
 */
@Data
public class CreateSceneWithFirstPromptRequest {

    // ---------- 场景字段（与 CreatePromptSceneRequest 一致） ----------
    @NotBlank(message = "场景编码不能为空")
    @Size(max = 64)
    private String code;

    @NotBlank(message = "场景名称不能为空")
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

    // ---------- 首条提示词字段 ----------
    @NotBlank(message = "首条提示词编码不能为空")
    @Size(max = 64)
    private String firstPromptCode;

    @NotBlank(message = "首条提示词名称不能为空")
    @Size(max = 100)
    private String firstPromptName;

    @NotBlank(message = "首条提示词正文不能为空")
    private String firstPromptContent;

    private String firstPromptContentType = "TEXT";

    @Size(max = 500)
    private String firstPromptDescription;

    private String firstPromptStatus = "ENABLED";

    // ---------- 该条在场景中的顺序与用法（场景项） ----------
    private Integer firstPromptDisplayOrder = 0;

    private String firstPromptUsageMode = "APPEND";
}
