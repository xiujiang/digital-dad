package com.digitaldad.user.controller;

import com.digitaldad.common.result.Result;
import com.digitaldad.user.dto.*;
import com.digitaldad.user.security.UserPrincipal;
import com.digitaldad.user.service.AdminHostService;
import com.digitaldad.user.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 超管 - 主持人管理
 * <p>提供主持人列表、创建、详情、状态管理、会员激活、配额调整及流水查询等功能。</p>
 */
@RestController
@RequestMapping("/api/admin/hosts")
@RequiredArgsConstructor
public class AdminHostController {

    private final AdminHostService adminHostService;
    private final MemberService memberService;

    /**
     * 分页列出主持人
     *
     * @param request 查询条件（手机号、状态、分页等）
     * @return 主持人列表分页数据
     */
    @GetMapping
    public Result<Page<AdminHostListItemResponse>> listHosts(
            @ModelAttribute AdminHostListRequest request) {
        Page<AdminHostListItemResponse> page = adminHostService.listHosts(request);
        return Result.ok(page);
    }

    /**
     * 创建主持人账号
     *
     * @param principal 当前登录的超管
     * @param request   主持人信息（手机号、昵称等）
     * @return 新建主持人详情
     */
    @PostMapping
    public Result<AdminHostDetailResponse> createHost(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AdminCreateHostRequest request) {
        AdminHostDetailResponse detail = adminHostService.createHost(request);
        return Result.ok(detail);
    }

    /**
     * 获取主持人详情
     *
     * @param id 主持人 ID
     * @return 主持人详情（含配额、会员等）
     */
    @GetMapping("/{id}")
    public Result<AdminHostDetailResponse> getHostDetail(@PathVariable Long id) {
        AdminHostDetailResponse detail = adminHostService.getHostDetail(id);
        return Result.ok(detail);
    }

    /**
     * 更新主持人状态（启用/禁用等）
     *
     * @param id      主持人 ID
     * @param request 状态值
     * @return 成功时返回空结果
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateHostStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateHostStatusRequest request) {
        adminHostService.updateHostStatus(id, request);
        return Result.ok();
    }

    /**
     * 为主持人激活会员套餐
     *
     * @param id      主持人 ID
     * @param request 套餐编码
     * @return 成功时返回空结果
     */
    @PostMapping("/{id}/activate-member")
    public Result<Void> activateMember(
            @PathVariable Long id,
            @Valid @RequestBody ActivateMemberRequest request) {
        memberService.activateMembership(id, request.getPackageCode());
        return Result.ok();
    }

    /**
     * 调整主持人配额
     *
     * @param principal 当前登录的超管
     * @param id        主持人 ID
     * @param request   调整量（正数增加、负数扣减）
     * @return 成功时返回空结果
     */
    @PutMapping("/{id}/quota")
    public Result<Void> adjustQuota(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AdminAdjustQuotaRequest request) {
        adminHostService.adjustQuota(id, request, principal.getUserId());
        return Result.ok();
    }

    /**
     * 获取主持人的配额流水
     *
     * @param id    主持人 ID
     * @param page  页码
     * @param size  每页条数
     * @return 配额流水分页数据
     */
    @GetMapping("/{id}/quota-flows")
    public Result<Page<QuotaFlowResponse>> getQuotaFlows(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<QuotaFlowResponse> flows = adminHostService.getQuotaFlows(id, page, size);
        return Result.ok(flows);
    }
}
