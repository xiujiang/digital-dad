package com.digitaldad.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增小结条目请求
 */
@Data
public class AddSummaryItemRequest {

    @NotBlank(message = "内容不能为空")
    @Size(max = 500)
    private String content;

    private String itemType = "FACT";
}
