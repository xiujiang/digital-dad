package com.digitaldad.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.Set;

/**
 * 当前登录用户主体（支持多角色）
 */
@Getter
@AllArgsConstructor
public class UserPrincipal {

    private final Long userId;
    private final Set<String> roles;

    public boolean isHost() {
        return roles != null && roles.contains("HOST");
    }

    public boolean isSuperAdmin() {
        return roles != null && roles.contains("SUPER_ADMIN");
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public Set<String> getRoles() {
        return roles != null ? roles : Collections.emptySet();
    }
}
