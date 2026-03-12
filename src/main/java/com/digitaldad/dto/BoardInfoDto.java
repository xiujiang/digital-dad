package com.digitaldad.project.dto;

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
}
