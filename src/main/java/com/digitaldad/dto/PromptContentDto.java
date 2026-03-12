package com.digitaldad.prompt.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 提示词内容 DTO（供给接口返回）
 */
@Data
@Builder
public class PromptContentDto {

    private Long templateId;
    private String templateCode;
    private Integer versionNo;
    private String content;
    private Integer displayOrder;
    private String usageMode;
}
