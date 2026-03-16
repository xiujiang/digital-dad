package com.digitaldad.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * C 端「我的板块」列表项：当前用户在该板块下有故事的项目板块
 * <p>用于按板块展示故事时，筛选用 projectBoardId 调用 GET /api/c/users/me/stories?projectBoardId=xxx</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBoardItemDto {

    /** 项目 ID */
    private Long projectId;
    /** 项目板块 ID，查该板块故事时传此参数 */
    private Long projectBoardId;
    private String boardCode;
    private String boardName;
    private Integer displayOrder;
}
