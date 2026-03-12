package com.digitaldad.board.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 板块元数据响应
 */
@Data
@Builder
public class BoardMetaResponse {

    private Long id;
    private String code;
    private String name;
    private Integer displayOrder;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
