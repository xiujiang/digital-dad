package com.digitaldad.project.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 绑定参与者响应
 */
@Data
@Builder
public class BindParticipantResponse {

    /** 参与者 ID */
    private Long participantId;

    /** 项目 ID */
    private Long projectId;

    /** 角色 */
    private String role;

    /** 采访入口 URL（绑定成功后跳转） */
    private String interviewUrl;
}
