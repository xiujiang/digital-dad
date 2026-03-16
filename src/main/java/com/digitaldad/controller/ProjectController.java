package com.digitaldad.controller;

import com.digitaldad.common.result.Result;
import com.digitaldad.dto.*;
import com.digitaldad.enums.ContentType;
import com.digitaldad.service.DeliverableService;
import com.digitaldad.service.ProjectService;
import com.digitaldad.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 统一项目接口（主持人 + 超管）
 * <p>列表/详情按角色：HOST 仅本人，SUPER_ADMIN 全部/任意；创建、分享、交付物仅项目归属或超管。</p>
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final DeliverableService deliverableService;

    /**
     * 分页列出项目：HOST 仅本人，SUPER_ADMIN 支持筛选查全部
     */
    @GetMapping
    public Result<?> listProjects(
            @AuthenticationPrincipal UserPrincipal principal,
            @ModelAttribute AdminProjectListRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (principal.hasRole("SUPER_ADMIN")) {
            if (request.getPage() == null) request.setPage(page);
            if (request.getSize() == null) request.setSize(size);
            Page<AdminProjectListItemResponse> adminPage = projectService.listProjectsForAdmin(request);
            return Result.ok(adminPage);
        }
        Page<ProjectListItemResponse> hostPage = projectService.listProjects(principal.getUserId(), page, size);
        return Result.ok(hostPage);
    }

    /**
     * 创建项目（主持人）
     */
    @PostMapping
    public Result<ProjectDetailResponse> createProject(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateProjectRequest request) {
        return Result.ok(projectService.createProject(principal.getUserId(), request));
    }

    /**
     * 更新项目：HOST 仅本人项目，SUPER_ADMIN 任意；仅更新请求中传入的字段
     */
    @PutMapping("/{id}")
    public Result<?> updateProject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request) {
        if (principal.hasRole("SUPER_ADMIN")) {
            return Result.ok(projectService.updateProjectForAdmin(id, request));
        }
        return Result.ok(projectService.updateProject(id, principal.getUserId(), request));
    }

    /**
     * 项目详情：HOST 仅本人项目，SUPER_ADMIN 任意
     */
    @GetMapping("/{id}")
    public Result<?> getProjectDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        if (principal.hasRole("SUPER_ADMIN")) {
            AdminProjectDetailResponse detail = projectService.getProjectDetailForAdmin(id);
            return Result.ok(detail);
        }
        ProjectDetailResponse detail = projectService.getProjectDetail(id, principal.getUserId());
        return Result.ok(detail);
    }

    /**
     * 项目分享入口：HOST 仅本人项目，SUPER_ADMIN 任意
     */
    @GetMapping("/{id}/share")
    public Result<ShareEntryResponse> getShareEntry(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        if (principal.hasRole("SUPER_ADMIN")) {
            return Result.ok(projectService.getShareEntryForAdmin(id));
        }
        return Result.ok(projectService.getShareEntry(id, principal.getUserId()));
    }

    /**
     * 生成交付物
     */
    @PostMapping("/{projectId}/deliverables/generate")
    public Result<DeliverableDetailResponse> generateDeliverable(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long projectId,
            @Valid @RequestBody GenerateDeliverableRequest request) {
        return Result.ok(deliverableService.generate(projectId, principal, request));
    }

    /**
     * 按内容类型获取项目交付物
     */
    @GetMapping("/{projectId}/deliverables/{contentType}")
    public Result<DeliverableDetailResponse> getDeliverableByType(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long projectId,
            @PathVariable ContentType contentType) {
        return Result.ok(deliverableService.getDetail(projectId, contentType, principal));
    }
}
