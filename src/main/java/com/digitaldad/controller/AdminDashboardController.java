package com.digitaldad.controller;

import com.digitaldad.common.result.Result;
import com.digitaldad.dto.DashboardOverviewResponse;
import com.digitaldad.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 超管 - 仪表盘（系统总览）
 * <p>对应 P01 系统总览页面，仅 SUPER_ADMIN 可访问。</p>
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * 获取仪表盘总览数据
     * <p>包含今日指标、成本估算、最近项目、总用户数、待处理内容等。</p>
     */
    @GetMapping("/dashboard")
    public Result<DashboardOverviewResponse> getDashboard() {
        return Result.ok(adminDashboardService.getOverview());
    }
}
