package com.digitaldad.user.controller;

import com.digitaldad.common.result.Result;
import com.digitaldad.user.dto.LoginRequest;
import com.digitaldad.user.dto.LoginResponse;
import com.digitaldad.user.dto.CurrentUserResponse;
import com.digitaldad.user.security.UserPrincipal;
import com.digitaldad.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 超管 - 认证接口
 * <p>提供超管登录及当前超管信息查询，仅限 SUPER_ADMIN 角色使用。</p>
 */
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;

    /**
     * 超管登录（手机号+验证码，仅限 SUPER_ADMIN 账号）
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.adminLogin(request.getPhone(), request.getCode());
        return Result.ok(response);
    }

    /**
     * 当前超管信息
     */
    @GetMapping("/me")
    public Result<CurrentUserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        CurrentUserResponse response = authService.getCurrentUser(principal.getUserId());
        return Result.ok(response);
    }
}
