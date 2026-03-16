package com.digitaldad.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建提示词版本请求
 */
@Data
public class CreatePromptVersionRequest {

    @NotBlank(message = "内容不能为空")
    private String content;

    /** 是否设为生效，默认 true */
    private Boolean setActive = true;
}
