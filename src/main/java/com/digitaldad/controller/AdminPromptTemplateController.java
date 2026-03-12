package com.digitaldad.prompt.controller;

import com.digitaldad.common.result.Result;
import com.digitaldad.prompt.dto.*;
import com.digitaldad.prompt.service.PromptTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 超管 - 提示词模板管理
 * <p>提供提示词模板的增删改查，模板是 prompt 的最小管理单元。</p>
 */
@RestController
@RequestMapping("/api/admin/prompt-templates")
@RequiredArgsConstructor
public class AdminPromptTemplateController {

    private final PromptTemplateService templateService;

    /**
     * 列出模板（支持按 contentType、status 筛选）
     *
     * @param contentType 内容类型
     * @param status      状态
     * @return 模板列表
     */
    @GetMapping
    public Result<List<PromptTemplateResponse>> list(
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String status) {
        return Result.ok(templateService.listAll(contentType, status));
    }

    /**
     * 获取模板详情
     *
     * @param id 模板 ID
     * @return 模板详情
     */
    @GetMapping("/{id}")
    public Result<PromptTemplateResponse> get(@PathVariable Long id) {
        return Result.ok(templateService.getById(id));
    }

    /**
     * 创建模板
     *
     * @param request 模板信息（编码、内容类型、描述等）
     * @return 新建的模板
     */
    @PostMapping
    public Result<PromptTemplateResponse> create(@Valid @RequestBody CreatePromptTemplateRequest request) {
        return Result.ok(templateService.create(request));
    }

    /**
     * 更新模板
     *
     * @param id      模板 ID
     * @param request 更新内容
     * @return 更新后的模板
     */
    @PutMapping("/{id}")
    public Result<PromptTemplateResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePromptTemplateRequest request) {
        return Result.ok(templateService.update(id, request));
    }

    /**
     * 删除模板
     *
     * @param id 模板 ID
     * @return 成功时返回空结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return Result.ok();
    }
}
