package com.digitaldad.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话响应（C 端不暴露参与者 ID）
 */
@Data
@Builder
public class SessionResponse {

    private Long id;
    private Long projectId;
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
    /** 当前用户在项目中的角色（GROOM/BRIDE），用于展示与提示词区分 */
    private String role;
}
