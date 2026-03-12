package com.digitaldad.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 绑定参与者请求（选择身份后）
 */
@Data
public class BindParticipantRequest {

    @NotBlank(message = "角色不能为空")
    private String role;  // GROOM / BRIDE
}
