package com.digitaldad.project.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理员 - 项目列表项响应（含主持人信息）
 */
@Data
@Builder
public class AdminProjectListItemResponse {

    private Long id;
    private String projectNo;
    private String groomName;
    private String brideName;
    private String theme;
    private LocalDate weddingDate;
    private String status;
    private LocalDateTime createdAt;

    /** 主持人ID */
    private Long hostUserId;
    /** 主持人姓名 */
    private String hostName;
    /** 主持人手机号 */
    private String hostPhone;
}
