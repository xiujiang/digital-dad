package com.digitaldad.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建项目请求
 */
@Data
public class CreateProjectRequest {

    @NotBlank(message = "新郎姓名不能为空")
    @Size(min = 2, max = 50)
    private String groomName;

    @NotBlank(message = "新娘姓名不能为空")
    @Size(min = 2, max = 50)
    private String brideName;

    private LocalDate weddingDate;

    /** 婚礼主题（如：婚礼故事采访、浪漫秋日婚礼等） */
    @Size(max = 200)
    private String theme;
}
