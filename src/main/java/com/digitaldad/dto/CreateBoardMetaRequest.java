package com.digitaldad.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建板块元数据请求
 */
@Data
public class CreateBoardMetaRequest {

    @NotBlank(message = "编码不能为空")
    @Size(max = 32)
    private String code;

    @NotBlank(message = "名称不能为空")
    @Size(max = 50)
    private String name;

    private Integer displayOrder = 0;

    @Size(max = 200)
    private String description;

    private String status = "ENABLED";
}
