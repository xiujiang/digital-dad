package com.digitaldad.project.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新小结条目请求
 */
@Data
public class UpdateSummaryItemRequest {

    @Size(max = 500)
    private String content;

    private String itemType;

    private Boolean isSelected;
}
