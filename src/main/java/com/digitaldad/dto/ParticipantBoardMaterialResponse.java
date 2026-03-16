package com.digitaldad.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 参与者在某板块下的最新素材快照（项目详情-参与者维度）
 */
@Data
@Builder
public class ParticipantBoardMaterialResponse {

    private Long projectBoardId;
    private String boardCode;
    private String boardName;
    private Integer displayOrder;

    /** 快照内容（JSON 数组，如 [{"content":"...","type":"..."}]） */
    private String snapshotPayload;
    private LocalDateTime snapshotCreatedAt;
    private Long summaryId;
}
