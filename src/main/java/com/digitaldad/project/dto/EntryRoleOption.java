package com.digitaldad.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * C 端入口页「选择身份」的单个角色选项（由后端返回，前端按此渲染）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntryRoleOption {

    /** 角色枚举值，如 GROOM、BRIDE */
    private String role;

    /** 展示名称，如 新郎、新娘 */
    private String label;

    /** 是否可选：true=未被占用可点选，false=已被占用仅展示 */
    private boolean available;
}
