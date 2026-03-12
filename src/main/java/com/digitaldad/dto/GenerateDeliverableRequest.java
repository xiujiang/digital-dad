package com.digitaldad.project.dto;

import com.digitaldad.project.enums.ContentType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 生成交付物请求
 */
@Data
public class GenerateDeliverableRequest {

    @NotNull(message = "交付物类型不能为空")
    private ContentType contentType;
}
