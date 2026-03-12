package com.digitaldad.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 调整配额请求
 */
@Data
public class AdminAdjustQuotaRequest {

    /** 变动量，正数增加，负数减少 */
    @NotNull(message = "变动量不能为空")
    private Integer delta;

    /** 变动原因 */
    private String reason;
}
