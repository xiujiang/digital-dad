package com.digitaldad.user.enums;

import lombok.Getter;

/**
 * 配额流水关联业务类型
 */
@Getter
public enum RefType {
    PROJECT("创建项目扣减"),
    ADMIN_ADJUST("管理员调整");

    private final String desc;

    RefType(String desc) {
        this.desc = desc;
    }
}
