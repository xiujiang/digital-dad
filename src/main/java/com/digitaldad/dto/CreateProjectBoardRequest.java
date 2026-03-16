package com.digitaldad.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 项目添加板块请求
 */
@Data
public class CreateProjectBoardRequest {

    @NotNull(message = "板块ID不能为空")
    private Long boardMetaId;

    private Integer displayOrder = 0;
}
