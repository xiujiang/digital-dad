package com.digitaldad.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 登录请求
 * <p>通过 admin 区分主持人登录与超管登录：不传或 false 为主持人，true 为超管。</p>
 */
@Data
public class LoginRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码为6位数字")
    private String code;

    /** 是否以超管身份登录，默认 false；true 时须具备 SUPER_ADMIN 角色 */
    private Boolean admin;
}
