package com.digitaldad.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增主持人请求
 */
@Data
public class AdminCreateHostRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "姓名不能为空")
    @Size(min = 2, max = 20)
    private String name;

    /** 初始配额，默认0 */
    private Integer initialQuota = 0;
}
