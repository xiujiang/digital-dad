package com.digitaldad.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新板块元数据请求
 */
@Data
public class UpdateBoardMetaRequest {

    @Size(max = 50)
    private String name;

    private Integer displayOrder;

    @Size(max = 200)
    private String description;

    private String status;
}
