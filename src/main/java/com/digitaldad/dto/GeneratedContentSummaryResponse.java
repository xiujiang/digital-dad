package com.digitaldad.dto;

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
    /** 是否基于最新素材快照生成：true=当前快照无更新，false=已有新快照待重新生成 */
    private Boolean usingLatestSnapshot;
    private Integer versionNo;  // 版本号
    private LocalDateTime updatedAt;
}
