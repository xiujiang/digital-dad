package com.digitaldad.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 提示词内容 DTO（供给接口返回）
 */
@Data
@Builder
public class PromptContentDto {

    private Long promptId;
    private String promptCode;
    private Integer versionNo;
    private String content;
    private Integer displayOrder;
    private String usageMode;
}
