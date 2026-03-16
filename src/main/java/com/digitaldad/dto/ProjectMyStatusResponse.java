package com.digitaldad.dto;

import com.digitaldad.enums.ProjectEntryStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * C 端「当前用户在当前项目下的状态」响应
 * <p>前端根据 step 及携带的 ID 决定展示选身份、开始采访、对话页、小结确认页或完成页。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMyStatusResponse {

    /** 当前步骤 */
    private ProjectEntryStep step;

    /** 是否已绑定该项目（选过身份） */
    private Boolean bound;

    /** 角色：GROOM / BRIDE */
    private String role;
    /** 参与者状态 */
    private String participantStatus;

    /** 会话 ID（有进行中或已完成会话时有） */
    private Long sessionId;
    /** 会话状态 */
    private String sessionStatus;

    /** 当前板块 ID */
    private Long currentProjectBoardId;
    private String boardCode;
    private String boardName;
    /** 当前板块在项目中的顺序（从 0 或 1 起，以实际为准） */
    private Integer currentBoardOrder;

    /** 项目下各板块简要信息（进度条用） */
    private List<BoardInfoDto> boards;

    /** 当前板块已用轮数、每块轮数上限 */
    private Integer currentBoardRoundCount;
    private Integer maxRoundsPerBoard;

    /** 当前待确认小结 ID（仅 WAITING_SUMMARY_CONFIRM 时有值） */
    private Long currentSummaryId;
    /** 当前小结状态 */
    private String currentSummaryStatus;
}
