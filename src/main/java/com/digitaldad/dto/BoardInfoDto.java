package com.digitaldad.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 板块简要信息
 */
@Data
@Builder
public class BoardInfoDto {

    private Long projectBoardId;
    private String boardCode;
    private String boardName;
    private Integer displayOrder;
    private Boolean isCurrent;
    private Boolean isCompleted;
    /** 当前板块对应的会话 ID（该用户在该板块下唯一会话，无则 null） */
    private Long sessionId;
    /** 是否已生成小结 */
    private Boolean hasSummary;
    /** 是否已生成故事 */
    private Boolean hasStory;
}
