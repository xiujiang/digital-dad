package com.digitaldad.project.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 项目信息（C端身份选择页使用）
 */
@Data
@Builder
public class ProjectInfoResponse {

    private Long projectId;
    private String shareToken;
    private String groomName;
    private String brideName;
    private LocalDate weddingDate;
}
