package com.digitaldad.user.controller;

import com.digitaldad.common.result.Result;
import com.digitaldad.user.dto.HostProfileResponse;
import com.digitaldad.user.dto.UpdateHostProfileRequest;
import com.digitaldad.user.security.UserPrincipal;
import com.digitaldad.user.service.HostUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 主持人 - 个人资料
 * <p>提供主持人个人资料的查询与更新。</p>
 */
@RestController
@RequestMapping("/api/b/users")
@RequiredArgsConstructor
public class HostUserController {

    private final HostUserService hostUserService;

    /**
     * 获取主持人资料
     *
     * @param principal 当前登录的主持人
     * @return 主持人资料
     */
    @GetMapping("/me")
    public Result<HostProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        HostProfileResponse profile = hostUserService.getProfile(principal.getUserId());
        return Result.ok(profile);
    }

    /**
     * 更新主持人资料
     *
     * @param principal 当前登录的主持人
     * @param request   更新内容（昵称等）
     * @return 成功时返回空结果
     */
    @PutMapping("/me")
    public Result<Void> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateHostProfileRequest request) {
        hostUserService.updateProfile(principal.getUserId(), request);
        return Result.ok();
    }
}
