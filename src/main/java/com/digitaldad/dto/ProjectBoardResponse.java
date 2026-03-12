package com.digitaldad.board.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目板块关联响应（含板块名称）
 */
@Data
@Builder
public class ProjectBoardResponse {

    private Long id;
    private Long projectId;
    private Long boardMetaId;
    private String boardCode;
    private String boardName;
    private Integer displayOrder;
    private LocalDateTime createdAt;
}
