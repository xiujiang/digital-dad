package com.digitaldad.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增关键人物请求
 */
@Data
public class AddKeyPersonRequest {

    @NotBlank(message = "人物称谓不能为空")
    @Size(max = 50)
    private String name;

    @Size(max = 50)
    private String roleLabel;
}
