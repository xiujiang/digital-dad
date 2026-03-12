package com.digitaldad.project.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交付物详情
 */
@Data
@Builder
public class DeliverableDetailResponse {

    private Long id;
    private Long projectId;
    private Long participantId;
    private String contentType;
    private String contentTypeName;
    private Integer versionNo;
    private String title;
    private String content;
    private String status;
    private LocalDateTime snapshotVersionAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
