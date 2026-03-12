package com.digitaldad.project.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目列表项响应
 */
@Data
@Builder
public class ProjectListItemResponse {

    private Long id;
    private String projectNo;
    private String groomName;
    private String brideName;
    private LocalDate weddingDate;
    private String status;
    private LocalDateTime createdAt;
}
