package com.digitaldad.controller;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.common.result.Result;
import com.digitaldad.dto.*;
import com.digitaldad.security.UserPrincipal;
import com.digitaldad.service.AdminHostService;
import com.digitaldad.service.AuthService;
import com.digitaldad.service.HostUserService;
import com.digitaldad.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 统一用户/主持人接口
 * <p>/api/users/me 共用；/api/users、/api/users/{id} 及子操作仅 SUPER_ADMIN。</p>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final HostUserService hostUserService;
    private final AdminHostService adminHostService;
    private final MemberService memberService;
    private final AuthService authService;

    /**
     * 当前用户资料（主持人返回 HostProfileResponse，超管返回 CurrentUserResponse）
     */
    @GetMapping("/me")
    public Result<?> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal.hasRole("HOST")) {
            HostProfileResponse profile = hostUserService.getProfile(principal.getUserId());
            return Result.ok(profile);
        }
        CurrentUserResponse current = authService.getCurrentUser(principal.getUserId());
        return Result.ok(current);
    }

    /**
     * 更新当前用户资料（主持人可更新昵称等）
     */
    @PutMapping("/me")
    public Result<Void> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateHostProfileRequest request) {
        if (!principal.hasRole("HOST")) {
            throw new BusinessException(403, "仅主持人可更新个人资料");
        }
        hostUserService.updateProfile(principal.getUserId(), request);
        return Result.ok();
    }

    /**
     * 用户/主持人列表（仅 SUPER_ADMIN）
     */
    @GetMapping
    public Result<Page<AdminHostListItemResponse>> listUsers(
            @AuthenticationPrincipal UserPrincipal principal,
            @ModelAttribute AdminHostListRequest request) {
        if (!principal.hasRole("SUPER_ADMIN")) {
            throw new BusinessException(403, "仅超管可查看用户列表");
        }
        return Result.ok(adminHostService.listHosts(request));
    }

    /**
     * 创建主持人（仅 SUPER_ADMIN）
     */
    @PostMapping
    public Result<AdminHostDetailResponse> createHost(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AdminCreateHostRequest request) {
        if (!principal.hasRole("SUPER_ADMIN")) {
            throw new BusinessException(403, "仅超管可创建主持人");
        }
        return Result.ok(adminHostService.createHost(request));
    }

    /**
     * 用户/主持人详情（仅 SUPER_ADMIN）
     */
    @GetMapping("/{id}")
    public Result<AdminHostDetailResponse> getUserDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        if (!principal.hasRole("SUPER_ADMIN")) {
            throw new BusinessException(403, "仅超管可查看用户详情");
        }
        return Result.ok(adminHostService.getHostDetail(id));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateUserStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateHostStatusRequest request) {
        if (!principal.hasRole("SUPER_ADMIN")) {
            throw new BusinessException(403, "仅超管可更新用户状态");
        }
        adminHostService.updateHostStatus(id, request);
        return Result.ok();
    }

    @PostMapping("/{id}/activate-member")
    public Result<Void> activateMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ActivateMemberRequest request) {
        if (!principal.hasRole("SUPER_ADMIN")) {
            throw new BusinessException(403, "仅超管可激活会员");
        }
        memberService.activateMembership(id, request.getPackageCode());
        return Result.ok();
    }

    @PutMapping("/{id}/quota")
    public Result<Void> adjustQuota(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AdminAdjustQuotaRequest request) {
        if (!principal.hasRole("SUPER_ADMIN")) {
            throw new BusinessException(403, "仅超管可调整配额");
        }
        adminHostService.adjustQuota(id, request, principal.getUserId());
        return Result.ok();
    }

    @GetMapping("/{id}/quota-flows")
    public Result<Page<QuotaFlowResponse>> getQuotaFlows(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (!principal.hasRole("SUPER_ADMIN")) {
            throw new BusinessException(403, "仅超管可查看配额流水");
        }
        return Result.ok(adminHostService.getQuotaFlows(id, page, size));
    }

    /**
     * 删除主持人（仅 SUPER_ADMIN，软删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteHost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        if (!principal.hasRole("SUPER_ADMIN")) {
            throw new BusinessException(403, "仅管理员可删除主持人");
        }
        adminHostService.deleteHost(id);
        return Result.ok();
    }
}
