package com.digitaldad.project.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 故事/时光响应
 */
@Data
@Builder
public class BoardStoryResponse {

    private Long id;
    private Long sessionId;
    private Long projectBoardId;
    private String boardCode;
    private String boardName;
    private String content;
    private Integer versionNo;
    private LocalDateTime createdAt;
}
