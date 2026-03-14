package com.digitaldad.project.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 项目信息（C 端身份选择页使用）
 * <p>选择身份的选项由后端通过 roleOptions 返回，前端按 roleOptions 渲染按钮/列表。</p>
 */
@Data
@Builder
public class ProjectInfoResponse {

    private Long projectId;
    private String shareToken;
    private String groomName;
    private String brideName;
    private String theme;
    private LocalDate weddingDate;

    /** 可选角色列表（如新郎、新娘），含是否已被占用；前端据此展示「选择身份」 */
    private List<EntryRoleOption> roleOptions;
}
