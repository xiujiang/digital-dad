package com.digitaldad.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private Long userId;
    private String userType;
    private List<String> roles;
    private String name;
    private String phone;
    /** 用户头像 URL（微信登录等场景返回） */
    private String avatarUrl;
}
