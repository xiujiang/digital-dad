package com.digitaldad.prompt.controller;

import com.digitaldad.common.result.Result;
import com.digitaldad.prompt.dto.*;
import com.digitaldad.prompt.service.PromptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 超管 - 提示词管理（列表/详情为当前生效，创建含首版正文）
 */
@RestController
@RequestMapping("/api/admin/prompts")
@RequiredArgsConstructor
public class AdminPromptController {

    private final PromptService promptService;

    /**
     * 列出当前生效的提示词（支持按 contentType、status 筛选）
     */
    @GetMapping
    public Result<List<PromptResponse>> list(
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String status) {
        return Result.ok(promptService.listAll(contentType, status));
    }

    /**
     * 按 id 获取提示词
     */
    @GetMapping("/{id}")
    public Result<PromptResponse> get(@PathVariable Long id) {
        return Result.ok(promptService.getById(id));
    }

    /**
     * 创建提示词（编码、名称、正文等，首版 version_no=1 且生效）
     */
    @PostMapping
    public Result<PromptResponse> create(@Valid @RequestBody CreatePromptRequest request) {
        return Result.ok(promptService.create(request));
    }

    /**
     * 更新提示词（按 id 更新该行）
     */
    @PutMapping("/{id}")
    public Result<PromptResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePromptRequest request) {
        return Result.ok(promptService.update(id, request));
    }

    /**
     * 删除提示词（按 id 删除该行；若该 code 被场景引用则禁止）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        promptService.delete(id);
        return Result.ok();
    }
}
