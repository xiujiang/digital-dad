package com.digitaldad.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设置/修改密码请求
 */
@Data
public class SetPasswordRequest {

    /** 新密码 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度为6-32位")
    private String newPassword;

    /** 原密码（修改时必填；首次设置时可不填） */
    private String oldPassword;
}
