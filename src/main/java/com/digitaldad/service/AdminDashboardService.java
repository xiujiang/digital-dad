package com.digitaldad.service;

import com.digitaldad.repository.SpeechTranscriptionUsageRepository;
import com.digitaldad.dto.DashboardOverviewResponse;
import com.digitaldad.entity.Project;
import com.digitaldad.enums.ContentStatus;
import com.digitaldad.repository.*;
import com.digitaldad.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 超管 - 仪表盘服务
 * <p>提供 P01 系统总览页面的统计数据。</p>
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ProjectRepository projectRepository;
    private final InterviewSessionRepository sessionRepository;
    private final GeneratedContentRepository generatedContentRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final SpeechTranscriptionUsageRepository speechUsageRepository;
    private final UserRepository userRepository;

    /**
     * 获取仪表盘总览数据
     */
    public DashboardOverviewResponse getOverview() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        // 模块 A：关键指标
        long todayProjects = projectRepository.countByCreatedAtBetweenAndStatusNotDisabled(start, end);
        long todayActiveSessions = sessionRepository.countActiveSessionsToday(start, end);
        long todayGenerations = generatedContentRepository.countByCreatedAtBetween(start, end);
        BigDecimal failureRate = BigDecimal.ZERO; // 首期暂无失败记录

        DashboardOverviewResponse.TodayMetrics todayMetrics = DashboardOverviewResponse.TodayMetrics.builder()
                .todayProjects(todayProjects)
                .todayActiveSessions(todayActiveSessions)
                .todayGenerations(todayGenerations)
                .failureRatePercent(failureRate)
                .build();

        // 模块 B：成本
        long speechSeconds = speechUsageRepository.sumDurationSecondsByCreatedAtBetween(start, end);
        long speechMinutes = speechSeconds / 60;
        long generatedToday = generatedContentRepository.countByCreatedAtBetween(start, end);
        long audioMessagesToday = conversationMessageRepository.countByCreatedAtBetweenAndAudioUrlNotNull(start, end);
        long storageObjectCount = generatedToday + audioMessagesToday;

        DashboardOverviewResponse.CostMetrics costMetrics = DashboardOverviewResponse.CostMetrics.builder()
                .speechMinutes(speechMinutes)
                .modelCallUsage(0L) // 首期暂无
                .storageObjectCount(storageObjectCount)
                .build();

        // 模块 C：最近失败（首期空列表）
        List<DashboardOverviewResponse.FailureRecordItem> recentFailures = Collections.emptyList();

        // 模块 D：最近项目
        List<Project> recentProjectList = projectRepository.findTop10ByOrderByCreatedAtDesc();
        List<DashboardOverviewResponse.RecentProjectItem> recentProjects = recentProjectList.stream()
                .map(p -> {
                    String title = buildProjectTitle(p.getGroomName(), p.getBrideName());
                    return DashboardOverviewResponse.RecentProjectItem.builder()
                            .id(p.getId())
                            .projectNo(p.getProjectNo())
                            .title(title)
                            .theme(p.getTheme())
                            .status(p.getStatus().name())
                            .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().format(DATE_TIME_FORMAT) : null)
                            .build();
                })
                .collect(Collectors.toList());

        // 总用户数、待处理内容
        long totalUsers = userRepository.count();
        long pendingContentCount = generatedContentRepository.countByStatus(ContentStatus.OUTDATED);

        return DashboardOverviewResponse.builder()
                .todayMetrics(todayMetrics)
                .costMetrics(costMetrics)
                .recentFailures(recentFailures)
                .recentProjects(recentProjects)
                .totalUsers(totalUsers)
                .pendingContentCount(pendingContentCount)
                .build();
    }

    private String buildProjectTitle(String groomName, String brideName) {
        if (groomName != null && brideName != null) {
            return groomName + " & " + brideName;
        }
        if (groomName != null) return groomName;
        if (brideName != null) return brideName;
        return "未命名项目";
    }
}
