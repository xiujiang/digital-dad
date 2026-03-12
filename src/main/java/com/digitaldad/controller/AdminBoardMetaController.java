package com.digitaldad.board.controller;

import com.digitaldad.board.dto.*;
import com.digitaldad.board.service.BoardMetaService;
import com.digitaldad.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 超管 - 板块元数据管理
 * <p>管理系统的板块元数据（如新郎、新娘等），供新建项目时选择使用。</p>
 */
@RestController
@RequestMapping("/api/admin/board-meta")
@RequiredArgsConstructor
public class AdminBoardMetaController {

    private final BoardMetaService boardMetaService;

    /**
     * 列出所有板块元数据
     *
     * @return 板块元数据列表
     */
    @GetMapping
    public Result<List<BoardMetaResponse>> list() {
        return Result.ok(boardMetaService.listAll());
    }

    /**
     * 获取板块元数据详情
     *
     * @param id 板块元数据 ID
     * @return 板块详情
     */
    @GetMapping("/{id}")
    public Result<BoardMetaResponse> get(@PathVariable Long id) {
        return Result.ok(boardMetaService.getById(id));
    }

    /**
     * 创建板块元数据
     *
     * @param request 板块信息（编码、名称等）
     * @return 新建的板块
     */
    @PostMapping
    public Result<BoardMetaResponse> create(@Valid @RequestBody CreateBoardMetaRequest request) {
        return Result.ok(boardMetaService.create(request));
    }

    /**
     * 更新板块元数据
     *
     * @param id      板块 ID
     * @param request 更新内容
     * @return 更新后的板块
     */
    @PutMapping("/{id}")
    public Result<BoardMetaResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBoardMetaRequest request) {
        return Result.ok(boardMetaService.update(id, request));
    }

    /**
     * 删除板块元数据
     *
     * @param id 板块 ID
     * @return 成功时返回空结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boardMetaService.delete(id);
        return Result.ok();
    }
}
