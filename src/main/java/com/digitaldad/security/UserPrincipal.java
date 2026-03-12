package com.digitaldad.user.security;

import com.digitaldad.user.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 当前登录用户主体
 */
@Getter
@AllArgsConstructor
public class UserPrincipal {

    private final Long userId;
    private final UserType userType;

    public boolean isHost() {
        return userType == UserType.HOST;
    }

    public boolean isSuperAdmin() {
        return userType == UserType.SUPER_ADMIN;
    }
}
