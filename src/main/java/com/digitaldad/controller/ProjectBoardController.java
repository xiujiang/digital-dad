package com.digitaldad.controller;

import com.digitaldad.dto.*;
import com.digitaldad.service.ProjectBoardService;
import com.digitaldad.common.result.Result;
import com.digitaldad.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 主持人 - 项目板块管理
 * <p>提供项目中板块的列表、新增、修改、删除，板块对应采访的不同主题（如新郎、新娘等）。</p>
 */
@RestController
@RequestMapping("/api/projects/{projectId}/boards")
@RequiredArgsConstructor
public class ProjectBoardController {

    private final ProjectBoardService projectBoardService;

    /**
     * 列出项目的所有板块
     *
     * @param projectId 项目 ID
     * @param principal  当前登录的主持人
     * @return 板块列表
     */
    @GetMapping
    public Result<List<ProjectBoardResponse>> list(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(projectBoardService.listByProject(projectId, principal.getUserId()));
    }

    /**
     * 为项目添加板块
     *
     * @param projectId 项目 ID
     * @param principal  当前登录的主持人
     * @param request    板块信息（boardCode、名称等）
     * @return 新增的板块
     */
    @PostMapping
    public Result<ProjectBoardResponse> add(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateProjectBoardRequest request) {
        return Result.ok(projectBoardService.addBoard(projectId, principal.getUserId(), request));
    }

    /**
     * 更新板块
     *
     * @param projectId      项目 ID
     * @param projectBoardId 板块 ID
     * @param principal      当前登录的主持人
     * @param request        更新内容
     * @return 更新后的板块
     */
    @PutMapping("/{projectBoardId}")
    public Result<ProjectBoardResponse> update(
            @PathVariable Long projectId,
            @PathVariable Long projectBoardId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProjectBoardRequest request) {
        return Result.ok(projectBoardService.update(projectId, projectBoardId, principal.getUserId(), request));
    }

    /**
     * 删除板块
     *
     * @param projectId      项目 ID
     * @param projectBoardId 板块 ID
     * @param principal      当前登录的主持人
     * @return 成功时返回空结果
     */
    @DeleteMapping("/{projectBoardId}")
    public Result<Void> remove(
            @PathVariable Long projectId,
            @PathVariable Long projectBoardId,
            @AuthenticationPrincipal UserPrincipal principal) {
        projectBoardService.remove(projectId, projectBoardId, principal.getUserId());
        return Result.ok();
    }
}
