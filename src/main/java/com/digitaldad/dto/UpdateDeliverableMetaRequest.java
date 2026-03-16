package com.digitaldad.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新交付物元数据请求
 */
@Data
public class UpdateDeliverableMetaRequest {

    @Size(max = 50)
    private String name;

    private Integer displayOrder;

    @Size(max = 200)
    private String description;

    private String status;
}
