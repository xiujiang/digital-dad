package com.digitaldad.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 当前用户信息响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserResponse {

    private Long userId;
    private String userType;
    private List<String> roles;
    private String name;
    private String phone;
    private String avatarUrl;
}
