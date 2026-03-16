package com.digitaldad.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目详情响应
 */
@Data
@Builder
public class ProjectDetailResponse {

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

    /** 成员列表（新郎、新娘的绑定状态与进度） */
    private List<ParticipantSummaryResponse> participants;

    /** 生成物列表 */
    private List<GeneratedContentSummaryResponse> contents;
}
