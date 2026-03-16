package com.digitaldad.dto;

import lombok.Data;

/**
 * 更新项目板块请求（调整顺序）
 */
@Data
public class UpdateProjectBoardRequest {

    private Integer displayOrder;
}
