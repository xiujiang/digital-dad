package com.digitaldad.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 开通会员请求
 */
@Data
public class ActivateMemberRequest {

    /** 套餐编码，从 sys_config member.packages 配置中读取，如 annual、single */
    @NotBlank(message = "套餐类型不能为空")
    @Size(min = 1, max = 32)
    private String packageCode;
}
