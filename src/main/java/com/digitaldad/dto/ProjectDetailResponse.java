package com.digitaldad.project.dto;

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
    private LocalDate weddingDate;
    private String status;
    private String shareToken;
    private LocalDateTime createdAt;

    /** 成员列表（新郎、新娘的绑定状态与进度） */
    private List<ParticipantSummaryResponse> participants;

    /** 生成物列表 */
    private List<GeneratedContentSummaryResponse> contents;
}
