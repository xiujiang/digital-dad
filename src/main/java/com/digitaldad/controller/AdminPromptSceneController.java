package com.digitaldad.controller;

import com.digitaldad.common.result.Result;
import com.digitaldad.dto.*;
import com.digitaldad.service.PromptSceneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 超管 - 提示词场景管理
 * <p>提供场景的增删改查及场景下条目的管理，场景用于组合多条提示词形成完整提示。</p>
 */
@RestController
@RequestMapping("/api/admin/prompt-scenes")
@RequiredArgsConstructor
public class AdminPromptSceneController {

    private final PromptSceneService sceneService;

    /**
     * 列出场景（支持按 scope、boardCode、roleType、status 筛选）
     *
     * @param scope      作用域
     * @param boardCode  板块编码
     * @param roleType   角色类型
     * @param status     状态
     * @return 场景列表
     */
    @GetMapping
    public Result<List<PromptSceneResponse>> list(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String boardCode,
            @RequestParam(required = false) String roleType,
            @RequestParam(required = false) String status) {
        return Result.ok(sceneService.listAll(scope, boardCode, roleType, status));
    }

    /**
     * 获取场景详情
     *
     * @param id 场景 ID
     * @return 场景详情
     */
    @GetMapping("/{id}")
    public Result<PromptSceneResponse> get(@PathVariable Long id) {
        return Result.ok(sceneService.getById(id));
    }

    /**
     * 创建场景并同时创建并绑定一条提示词（首版）
     * <p>一次请求完成：创建场景 → 创建提示词（version_no=1）→ 添加场景项。返回带条目的场景详情。</p>
     *
     * @param request 场景字段 + 首条提示词字段 + 该条在场景中的顺序与用法
     * @return 新建的场景（含已绑定的首条提示词）
     */
    @PostMapping("/with-first-prompt")
    public Result<PromptSceneResponse> createWithFirstPrompt(
            @Valid @RequestBody CreateSceneWithFirstPromptRequest request) {
        return Result.ok(sceneService.createWithFirstPrompt(request));
    }

    /**
     * 创建场景
     *
     * @param request 场景信息
     * @return 新建的场景
     */
    @PostMapping
    public Result<PromptSceneResponse> create(@Valid @RequestBody CreatePromptSceneRequest request) {
        return Result.ok(sceneService.create(request));
    }

    /**
     * 更新场景
     *
     * @param id      场景 ID
     * @param request 更新内容
     * @return 更新后的场景
     */
    @PutMapping("/{id}")
    public Result<PromptSceneResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePromptSceneRequest request) {
        return Result.ok(sceneService.update(id, request));
    }

    /**
     * 删除场景
     *
     * @param id 场景 ID
     * @return 成功时返回空结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sceneService.delete(id);
        return Result.ok();
    }

    /**
     * 向场景添加条目（关联提示词及顺序）
     *
     * @param id      场景 ID
     * @param request 条目信息
     * @return 新增的条目
     */
    @PostMapping("/{id}/items")
    public Result<PromptSceneItemResponse> addItem(
            @PathVariable Long id,
            @Valid @RequestBody AddSceneItemRequest request) {
        return Result.ok(sceneService.addItem(id, request));
    }

    /**
     * 更新场景条目
     *
     * @param itemId  条目 ID
     * @param request 更新内容
     * @return 更新后的条目
     */
    @PutMapping("/items/{itemId}")
    public Result<PromptSceneItemResponse> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateSceneItemRequest request) {
        return Result.ok(sceneService.updateItem(itemId, request));
    }

    /**
     * 移除场景条目
     *
     * @param itemId 条目 ID
     * @return 成功时返回空结果
     */
    @DeleteMapping("/items/{itemId}")
    public Result<Void> removeItem(@PathVariable Long itemId) {
        sceneService.removeItem(itemId);
        return Result.ok();
    }
}
