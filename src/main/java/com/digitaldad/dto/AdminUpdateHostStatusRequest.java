package com.digitaldad.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新主持人状态请求
 */
@Data
public class AdminUpdateHostStatusRequest {

    @NotBlank(message = "状态不能为空")
    private String status; // ENABLED / DISABLED
}
