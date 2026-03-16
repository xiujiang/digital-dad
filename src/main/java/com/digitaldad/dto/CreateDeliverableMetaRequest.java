package com.digitaldad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建交付物元数据请求
 */
@Data
public class CreateDeliverableMetaRequest {

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
