package com.digitaldad.project.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话响应
 */
@Data
@Builder
public class SessionResponse {

    private Long id;
    private Long projectId;
    private Long participantId;
    private Long currentProjectBoardId;
    private String boardCode;
    private String boardName;
    private String status;
    private Integer roundCount;
    /** 当前板块已用轮数 */
    private Integer currentBoardRoundCount;
    /** 板块轮数上限 */
    private Integer maxRoundsPerBoard;
    private LocalDateTime startedAt;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;

    private Integer currentBoardOrder;
    private List<BoardInfoDto> boards;
}
