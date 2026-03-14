package com.digitaldad.prompt.controller;

import com.digitaldad.common.result.Result;
import com.digitaldad.prompt.dto.*;
import com.digitaldad.prompt.service.PromptService;
import com.digitaldad.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 超管 - 提示词版本管理（按 code 管理多版本）
 */
@RestController
@RequestMapping("/api/admin/prompts/{code}/versions")
@RequiredArgsConstructor
public class AdminPromptVersionController {

    private final PromptService promptService;

    /**
     * 列出该提示词（code）下的所有版本
     */
    @GetMapping
    public Result<List<PromptVersionResponse>> list(@PathVariable String code) {
        return Result.ok(promptService.listVersionsByCode(code));
    }

    /**
     * 获取版本详情（按版本 id）
     */
    @GetMapping("/{versionId}")
    public Result<PromptVersionResponse> get(@PathVariable Long versionId) {
        return Result.ok(promptService.getVersionById(versionId));
    }

    /**
     * 创建新版本（同 code，version_no 递增）
     */
    @PostMapping
    public Result<PromptVersionResponse> create(
            @PathVariable String code,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePromptVersionRequest request) {
        Long userId = principal != null ? principal.getUserId() : null;
        return Result.ok(promptService.createVersion(code, request, userId));
    }

    /**
     * 激活指定版本
     */
    @PutMapping("/{versionId}/activate")
    public Result<PromptVersionResponse> activate(@PathVariable Long versionId) {
        return Result.ok(promptService.activateVersion(versionId));
    }

    /**
     * 删除版本（生效中的不能删）
     */
    @DeleteMapping("/{versionId}")
    public Result<Void> delete(@PathVariable Long versionId) {
        promptService.deleteVersion(versionId);
        return Result.ok();
    }
}
