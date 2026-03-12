package com.digitaldad.project.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 参与者摘要（项目详情中的成员信息）
 */
@Data
@Builder
public class ParticipantSummaryResponse {

    private Long id;
    private String role;      // GROOM / BRIDE（兼容 ProjectService）
    private String roleType;  // GROOM / BRIDE
    private String roleName;  // 新郎 / 新娘
    private String status;
    private Integer currentBoardOrder;  // 当前板块 1-4
    private LocalDateTime joinedAt;
    private LocalDateTime lastActiveAt;
    /** 是否已绑定用户 */
    private Boolean bound;
}
