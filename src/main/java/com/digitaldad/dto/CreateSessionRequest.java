package com.digitaldad.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建/恢复会话请求（绑定+进入合并后）
 * <p>传 projectId + projectBoardId 表示进入该项目下某板块的采访；若用户尚未绑定该项目，则 role 必填（GROOM/BRIDE）。</p>
 */
@Data
public class CreateSessionRequest {

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    /** 板块 ID（必填），表示进入该项目下哪一个板块的采访，换板块会产生新 session */
    @NotNull(message = "板块ID不能为空")
    private Long projectBoardId;

    /** 角色（新郎/新娘），未绑定时必填，已绑定可省略 */
    private String role;
}
