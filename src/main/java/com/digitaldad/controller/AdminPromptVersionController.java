package com.digitaldad.prompt.controller;

import com.digitaldad.common.result.Result;
import com.digitaldad.prompt.dto.*;
import com.digitaldad.prompt.service.PromptVersionService;
import com.digitaldad.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 超管 - 提示词版本管理
 * <p>提供提示词模板的版本列表、创建、激活、删除等操作。</p>
 */
@RestController
@RequestMapping("/api/admin/prompt-templates/{templateId}/versions")
@RequiredArgsConstructor
public class AdminPromptVersionController {

    private final PromptVersionService versionService;

    /**
     * 列出模板下的所有版本
     *
     * @param templateId 模板 ID
     * @return 版本列表
     */
    @GetMapping
    public Result<List<PromptVersionResponse>> list(@PathVariable Long templateId) {
        return Result.ok(versionService.listByTemplate(templateId));
    }

    /**
     * 获取版本详情
     *
     * @param versionId 版本 ID
     * @return 版本详情
     */
    @GetMapping("/{versionId}")
    public Result<PromptVersionResponse> get(@PathVariable Long versionId) {
        return Result.ok(versionService.getById(versionId));
    }

    /**
     * 创建新版本
     *
     * @param templateId 模板 ID
     * @param principal  当前登录用户（可选）
     * @param request    版本内容
     * @return 新建的版本
     */
    @PostMapping
    public Result<PromptVersionResponse> create(
            @PathVariable Long templateId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePromptVersionRequest request) {
        Long userId = principal != null ? principal.getUserId() : null;
        return Result.ok(versionService.create(templateId, request, userId));
    }

    /**
     * 激活指定版本（使之生效）
     *
     * @param versionId 版本 ID
     * @return 激活后的版本
     */
    @PutMapping("/{versionId}/activate")
    public Result<PromptVersionResponse> activate(@PathVariable Long versionId) {
        return Result.ok(versionService.activate(versionId));
    }

    /**
     * 删除版本
     *
     * @param versionId 版本 ID
     * @return 成功时返回空结果
     */
    @DeleteMapping("/{versionId}")
    public Result<Void> delete(@PathVariable Long versionId) {
        versionService.delete(versionId);
        return Result.ok();
    }
}
