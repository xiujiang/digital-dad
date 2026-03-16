package com.digitaldad.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 更新项目请求（仅更新传入的字段，未传则不修改）
 */
@Data
public class UpdateProjectRequest {

    @Size(min = 2, max = 50)
    private String groomName;

    @Size(min = 2, max = 50)
    private String brideName;

    private LocalDate weddingDate;

    /** 婚礼主题 */
    @Size(max = 200)
    private String theme;

    /** 联系方式（与项目绑定） */
    @Size(max = 100)
    private String contactInfo;
}
