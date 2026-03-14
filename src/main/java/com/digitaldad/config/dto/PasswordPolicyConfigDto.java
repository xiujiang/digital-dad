package com.digitaldad.config.dto;

import lombok.Data;

/**
 * 密码策略配置
 */
@Data
public class PasswordPolicyConfigDto {

    /** 强制强密码（至少8位，含大小写字母、数字、特殊字符） */
    private Boolean enforceStrongPassword = true;

    /** 定期修改密码 */
    private Boolean requirePasswordChangePeriodically = false;

    /** 密码有效期天数（启用定期修改时生效），默认90天 */
    private Integer passwordChangeIntervalDays = 90;
}
