package com.digitaldad.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建/恢复会话请求
 */
@Data
public class CreateSessionRequest {

    @NotNull(message = "参与者ID不能为空")
    private Long participantId;
}
