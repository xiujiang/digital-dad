package com.digitaldad.dto;

import lombok.Data;

/**
 * 更新场景项请求（顺序、使用方式）
 */
@Data
public class UpdateSceneItemRequest {

    private Integer displayOrder;
    private String usageMode;
}
