package com.digitaldad.user.controller;

import com.digitaldad.common.result.Result;
import com.digitaldad.user.dto.*;
import com.digitaldad.user.security.UserPrincipal;
import com.digitaldad.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 * <p>提供 C 端/B 端用户的验证码发送、登录、退出及当前用户信息查询。</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 发送验证码
     *
     * @param request 手机号
     * @return 成功时返回空结果
     */
    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        authService.sendCode(request.getPhone());
        return Result.ok();
    }

    /**
     * 用户登录（手机号 + 验证码）
     *
     * @param request 手机号与验证码
     * @return 登录结果（Token、用户信息）
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request.getPhone(), request.getCode());
        return Result.ok(response);
    }

    /**
     * 用户退出登录
     *
     * @return 成功时返回空结果
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    /**
     * 获取当前登录用户信息
     *
     * @param principal 当前用户（从 Token 解析）
     * @return 用户信息
     */
    @GetMapping("/me")
    public Result<CurrentUserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        CurrentUserResponse response = authService.getCurrentUser(principal.getUserId());
        return Result.ok(response);
    }
}
