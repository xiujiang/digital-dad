package com.digitaldad.project.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 生成物摘要（项目详情中的生成物信息）
 */
@Data
@Builder
public class GeneratedContentSummaryResponse {

    private Long id;
    private String contentType;  // OPENING_SPEECH / GROOM_VOW / BRIDE_VOW
    private String contentTypeName;  // 开场白 / 新郎誓言 / 新娘誓言
    private String title;  // 标题
    private String status;  // DRAFT / ACTIVE / OUTDATED
    private Integer versionNo;  // 版本号
    private LocalDateTime updatedAt;
}
