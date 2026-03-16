package com.digitaldad.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交付物元数据响应
 */
@Data
@Builder
public class DeliverableMetaResponse {

    private Long id;
    private String code;
    private String name;
    private Integer displayOrder;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
