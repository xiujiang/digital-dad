package com.digitaldad.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 绑定参与者响应（C 端不暴露参与者 ID，仅项目与角色）
 */
@Data
@Builder
public class BindParticipantResponse {

    /** 项目 ID */
    private Long projectId;

    /** 角色 */
    private String role;

    /** 采访入口 URL（绑定成功后跳转） */
    private String interviewUrl;
}
