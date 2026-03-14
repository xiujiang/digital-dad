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
     * <p>通过 request.admin 区分：true 为超管登录（须具备 SUPER_ADMIN），否则为主持人登录。</p>
     *
     * @param request 手机号、验证码及可选 admin
     * @return 登录结果（Token、用户信息）
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = Boolean.TRUE.equals(request.getAdmin())
                ? authService.adminLogin(request.getPhone(), request.getCode())
                : authService.login(request.getPhone(), request.getCode());
        return Result.ok(response);
    }

    /**
     * 用户登录（手机号 + 密码）
     * <p>支持主持人和超管；须已通过验证码登录后设置过密码。</p>
     *
     * @param request 手机号、密码及可选 admin
     * @return 登录结果（Token、用户信息）
     */
    @PostMapping("/login-password")
    public Result<LoginResponse> loginWithPassword(@Valid @RequestBody PasswordLoginRequest request) {
        LoginResponse response = Boolean.TRUE.equals(request.getAdmin())
                ? authService.adminLoginWithPassword(request.getPhone(), request.getPassword())
                : authService.loginWithPassword(request.getPhone(), request.getPassword());
        return Result.ok(response);
    }

    /**
     * 微信小程序登录（小程序将 wx.login() 得到的 code 发到后端，后端用 code 换 openid 并签发 JWT）
     *
     * @param request 含 code
     * @return 登录结果（Token、用户信息），与手机号登录结构一致
     */
    @PostMapping("/wechat-login")
    public Result<LoginResponse> wechatLogin(@Valid @RequestBody WeChatLoginRequest request) {
        return Result.ok(authService.wechatLogin(request.getCode()));
    }

    /**
     * 设置或修改密码（需已登录）
     * <p>首次设置可不填 oldPassword；修改时须填原密码。</p>
     *
     * @param request 新密码及可选原密码
     * @return 成功时返回空结果
     */
    @PostMapping("/set-password")
    public Result<Void> setPassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SetPasswordRequest request) {
        authService.setPassword(principal.getUserId(), request.getNewPassword(), request.getOldPassword());
        return Result.ok();
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
