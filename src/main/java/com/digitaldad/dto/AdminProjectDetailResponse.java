package com.digitaldad.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员 - 项目详情响应（含主持人信息）
 */
@Data
@Builder
public class AdminProjectDetailResponse {

    private Long id;
    private String projectNo;
    private String groomName;
    private String brideName;
    private String theme;
    private LocalDate weddingDate;
    /** 联系方式（与项目绑定） */
    private String contactInfo;
    private String status;
    private String shareToken;
    private LocalDateTime createdAt;

    /** 主持人ID */
    private Long hostUserId;
    /** 主持人姓名 */
    private String hostName;
    /** 主持人手机号 */
    private String hostPhone;

    /** 成员列表 */
    private List<ParticipantSummaryResponse> participants;
    /** 生成物列表 */
    private List<GeneratedContentSummaryResponse> contents;
}
