package com.digitaldad.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交付物详情（C 端不暴露参与者 ID）
 */
@Data
@Builder
public class DeliverableDetailResponse {

    private Long id;
    private Long projectId;
    private String contentType;
    private String contentTypeName;
    private Integer versionNo;
    private String title;
    private String content;
    private String status;
    /** 是否基于最新素材快照生成 */
    private Boolean usingLatestSnapshot;
    private LocalDateTime snapshotVersionAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
