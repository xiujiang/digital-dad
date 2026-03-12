package com.digitaldad.project.controller;

import com.digitaldad.common.result.Result;
import com.digitaldad.project.dto.*;
import com.digitaldad.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 超管 - 项目管理
 * <p>提供全平台项目的列表与详情查询，用于运营监控与排查。</p>
 */
@RestController
@RequestMapping("/api/admin/projects")
@RequiredArgsConstructor
public class AdminProjectController {

    private final ProjectService projectService;

    /**
     * 分页列出所有项目
     *
     * @param request 查询条件（分页、筛选等）
     * @return 项目列表分页数据
     */
    @GetMapping
    public Result<Page<AdminProjectListItemResponse>> listProjects(
            @ModelAttribute AdminProjectListRequest request) {
        Page<AdminProjectListItemResponse> page = projectService.listProjectsForAdmin(request);
        return Result.ok(page);
    }

    /**
     * 获取项目详情（超管视角，含完整信息）
     *
     * @param id 项目 ID
     * @return 项目详情
     */
    @GetMapping("/{id}")
    public Result<AdminProjectDetailResponse> getProjectDetail(@PathVariable Long id) {
        AdminProjectDetailResponse detail = projectService.getProjectDetailForAdmin(id);
        return Result.ok(detail);
    }
}
