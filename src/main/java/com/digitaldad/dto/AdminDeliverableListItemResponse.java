package com.digitaldad.project.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员 - 交付物列表项响应（含主持人生成人信息）
 */
@Data
@Builder
public class AdminDeliverableListItemResponse {

    private Long id;
    private Long projectId;
    private String projectNo;
    private String title;
    private String contentType;
    private String contentTypeName;
    private String status;

    /** 主持人生成人 ID */
    private Long hostUserId;
    /** 主持人生成人名称（作者） */
    private String hostName;

    private LocalDateTime createdAt;
}
