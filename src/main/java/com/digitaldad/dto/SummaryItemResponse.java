package com.digitaldad.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小结条目响应
 */
@Data
@Builder
public class SummaryItemResponse {

    private Long id;
    private Long summaryId;
    private String itemType;
    private String content;
    private Integer itemOrder;
    private Boolean isSelected;
    private LocalDateTime createdAt;
}
