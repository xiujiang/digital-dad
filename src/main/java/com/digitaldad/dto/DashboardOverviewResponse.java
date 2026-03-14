package com.digitaldad.project.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理员 - 仪表盘总览响应
 * <p>对应 P01 系统总览页面数据。</p>
 */
@Data
@Builder
public class DashboardOverviewResponse {

    /** 模块 A：关键指标卡（今日概览） */
    private TodayMetrics todayMetrics;

    /** 模块 B：成本估算 */
    private CostMetrics costMetrics;

    /** 模块 C：最近失败记录（首期返回空列表，暂无失败记录表） */
    private List<FailureRecordItem> recentFailures;

    /** 模块 D：最近项目 */
    private List<RecentProjectItem> recentProjects;

    /** 总用户数 */
    private long totalUsers;

    /** 待处理内容数（OUTDATED） */
    private long pendingContentCount;

    @Data
    @Builder
    public static class TodayMetrics {
        /** 今日新增项目数 */
        private long todayProjects;
        /** 今日采访会话数（活跃） */
        private long todayActiveSessions;
        /** 今日生成次数 */
        private long todayGenerations;
        /** 失败率（0-100），首期暂无失败记录返回 0 */
        private BigDecimal failureRatePercent;
    }

    @Data
    @Builder
    public static class CostMetrics {
        /** 语音识别分钟数（今日） */
        private long speechMinutes;
        /** 模型调用消耗量（今日），首期暂无数据返回 0 */
        private long modelCallUsage;
        /** 存储对象数（今日新增），近似口径 */
        private long storageObjectCount;
    }

    @Data
    @Builder
    public static class FailureRecordItem {
        private String failureId;
        private String projectNo;
        private String failedAt;
        private String reasonSummary;
    }

    @Data
    @Builder
    public static class RecentProjectItem {
        private Long id;
        private String projectNo;
        private String title;
        private String theme;
        private String status;
        private String createdAt;
    }
}
