package com.digitaldad.project.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 板块小结响应
 */
@Data
@Builder
public class BoardSummaryResponse {

    private Long id;
    private Long sessionId;
    private Long projectBoardId;
    private String boardCode;
    private String boardName;
    private Integer versionNo;
    private String status;
    private String title;
    private LocalDateTime generatedAt;
    private LocalDateTime confirmedAt;

    private List<SummaryItemResponse> items;
}
