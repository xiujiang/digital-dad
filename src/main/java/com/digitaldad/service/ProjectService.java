package com.digitaldad.project.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.project.dto.*;
import com.digitaldad.project.entity.GeneratedContent;
import com.digitaldad.project.entity.Project;
import com.digitaldad.project.enums.ContentType;
import com.digitaldad.project.entity.ProjectParticipant;
import com.digitaldad.project.enums.ParticipantRole;
import com.digitaldad.project.enums.ParticipantStatus;
import com.digitaldad.project.enums.ProjectStatus;
import com.digitaldad.project.repository.GeneratedContentRepository;
import com.digitaldad.project.repository.ProjectParticipantRepository;
import com.digitaldad.project.repository.ProjectRepository;
import com.digitaldad.user.entity.User;
import com.digitaldad.user.enums.QuotaType;
import com.digitaldad.user.repository.UserRepository;
import com.digitaldad.user.enums.RefType;
import com.digitaldad.user.service.UserQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 项目服务
 * <p>管理婚礼项目的创建、列表、详情、分享入口，以及超管视角的项目查询。</p>
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectParticipantRepository participantRepository;
    private final GeneratedContentRepository generatedContentRepository;
    private final DeliverableService deliverableService;
    private final UserQuotaService userQuotaService;
    private final UserRepository userRepository;

    private static final Map<ContentType, String> CONTENT_TYPE_NAMES = Map.of(
            ContentType.OPENING_SPEECH, "婚礼开场白",
            ContentType.GROOM_VOW, "新郎誓言",
            ContentType.BRIDE_VOW, "新娘誓言"
    );

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * 创建项目
     */
    @Transactional
    public ProjectDetailResponse createProject(Long hostUserId, CreateProjectRequest request) {
        userQuotaService.checkQuota(hostUserId, QuotaType.PROJECT, 1);
        userQuotaService.deduct(hostUserId, QuotaType.PROJECT, RefType.PROJECT, null);

        String projectNo = "P" + java.time.LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + String.format("%04d", (int) (Math.random() * 10000));
        while (projectRepository.existsByProjectNoAndDeletedAtIsNull(projectNo)) {
            projectNo = "P" + java.time.LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                    + String.format("%04d", (int) (Math.random() * 10000));
        }

        String shareToken = UUID.randomUUID().toString().replace("-", "");

        Project project = new Project();
        project.setProjectNo(projectNo);
        project.setHostUserId(hostUserId);
        project.setGroomName(request.getGroomName());
        project.setBrideName(request.getBrideName());
        project.setWeddingDate(request.getWeddingDate());
        project.setShareToken(shareToken);
        project = projectRepository.save(project);

        return getProjectDetail(project.getId(), hostUserId);
    }

    /**
     * 项目列表（主持人自己的项目）
     */
    public Page<ProjectListItemResponse> listProjects(Long hostUserId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(50, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return projectRepository.findByHostUserIdAndDeletedAtIsNull(hostUserId, pageable)
                .map(this::toListItem);
    }

    /**
     * 项目详情
     */
    public ProjectDetailResponse getProjectDetail(Long projectId, Long hostUserId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(404, "项目不存在"));
        if (!project.getHostUserId().equals(hostUserId)) {
            throw new BusinessException(403, "无权限访问该项目");
        }

        deliverableService.refreshOutdatedStatusForProject(projectId);

        List<ProjectParticipant> participants = participantRepository.findByProjectId(projectId);
        List<ParticipantSummaryResponse> participantSummaries = participants.stream()
                .map(this::toParticipantSummary)
                .collect(Collectors.toList());

        List<GeneratedContent> contents = generatedContentRepository.findByProjectId(projectId);
        List<GeneratedContentSummaryResponse> contentSummaries = contents.stream()
                .map(this::toContentSummary)
                .collect(Collectors.toList());

        return ProjectDetailResponse.builder()
                .id(project.getId())
                .projectNo(project.getProjectNo())
                .groomName(project.getGroomName())
                .brideName(project.getBrideName())
                .weddingDate(project.getWeddingDate())
                .status(project.getStatus().name())
                .shareToken(project.getShareToken())
                .participants(participantSummaries)
                .contents(contentSummaries)
                .createdAt(project.getCreatedAt())
                .build();
    }

    /**
     * 分享入口
     */
    public ShareEntryResponse getShareEntry(Long projectId, Long hostUserId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(404, "项目不存在"));
        if (!project.getHostUserId().equals(hostUserId)) {
            throw new BusinessException(403, "无权限访问该项目");
        }

        String entryUrl = baseUrl + "/c/entry/" + project.getShareToken();
        return ShareEntryResponse.builder()
                .projectId(project.getId())
                .shareToken(project.getShareToken())
                .entryUrl(entryUrl)
                .qrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + java.net.URLEncoder.encode(entryUrl, java.nio.charset.StandardCharsets.UTF_8))
                .build();
    }

    /**
     * 根据 share_token 获取项目信息（C 端扫码进入用）
     */
    public ProjectInfoResponse getProjectInfoByToken(String shareToken) {
        Project project = projectRepository.findByShareTokenAndDeletedAtIsNull(shareToken)
                .orElseThrow(() -> new BusinessException(404, "链接无效或已过期"));
        return ProjectInfoResponse.builder()
                .projectId(project.getId())
                .groomName(project.getGroomName())
                .brideName(project.getBrideName())
                .weddingDate(project.getWeddingDate())
                .shareToken(shareToken)
                .build();
    }

    /**
     * 根据 share_token 获取项目（内部用）
     */
    public Project getProjectByShareToken(String shareToken) {
        return projectRepository.findByShareTokenAndDeletedAtIsNull(shareToken)
                .orElseThrow(() -> new BusinessException(404, "链接无效或已过期"));
    }

    /**
     * 管理员 - 项目列表（支持按主持人、状态、关键词筛选，查全部）
     */
    public Page<AdminProjectListItemResponse> listProjectsForAdmin(AdminProjectListRequest request) {
        int page = request.getPage() != null ? Math.max(1, request.getPage()) : 1;
        int size = request.getSize() != null ? Math.min(50, Math.max(1, request.getSize())) : 10;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        com.digitaldad.project.enums.ProjectStatus statusEnum = null;
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                statusEnum = com.digitaldad.project.enums.ProjectStatus.valueOf(request.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        String keyword = (request.getKeyword() != null && !request.getKeyword().isBlank())
                ? request.getKeyword().trim() : null;

        return projectRepository.findByFiltersForAdmin(
                        request.getHostUserId(), statusEnum, keyword, pageable)
                .map(this::toAdminListItem);
    }

    /**
     * 管理员 - 项目详情（可查看任意项目，含主持人信息）
     */
    public AdminProjectDetailResponse getProjectDetailForAdmin(Long projectId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(404, "项目不存在"));

        User host = userRepository.findById(project.getHostUserId()).orElse(null);
        String hostName = host != null ? host.getName() : null;
        String hostPhone = host != null ? host.getPhone() : null;

        List<ProjectParticipant> participants = participantRepository.findByProjectId(projectId);
        List<ParticipantSummaryResponse> participantSummaries = participants.stream()
                .map(this::toParticipantSummary)
                .collect(Collectors.toList());

        List<GeneratedContent> contents = generatedContentRepository.findByProjectId(projectId);
        List<GeneratedContentSummaryResponse> contentSummaries = contents.stream()
                .map(this::toContentSummary)
                .collect(Collectors.toList());

        return AdminProjectDetailResponse.builder()
                .id(project.getId())
                .projectNo(project.getProjectNo())
                .groomName(project.getGroomName())
                .brideName(project.getBrideName())
                .weddingDate(project.getWeddingDate())
                .status(project.getStatus().name())
                .shareToken(project.getShareToken())
                .createdAt(project.getCreatedAt())
                .hostUserId(project.getHostUserId())
                .hostName(hostName)
                .hostPhone(hostPhone)
                .participants(participantSummaries)
                .contents(contentSummaries)
                .build();
    }

    private AdminProjectListItemResponse toAdminListItem(Project p) {
        User host = userRepository.findById(p.getHostUserId()).orElse(null);
        return AdminProjectListItemResponse.builder()
                .id(p.getId())
                .projectNo(p.getProjectNo())
                .groomName(p.getGroomName())
                .brideName(p.getBrideName())
                .weddingDate(p.getWeddingDate())
                .status(p.getStatus().name())
                .createdAt(p.getCreatedAt())
                .hostUserId(p.getHostUserId())
                .hostName(host != null ? host.getName() : null)
                .hostPhone(host != null ? host.getPhone() : null)
                .build();
    }

    private ProjectListItemResponse toListItem(Project p) {
        return ProjectListItemResponse.builder()
                .id(p.getId())
                .projectNo(p.getProjectNo())
                .groomName(p.getGroomName())
                .brideName(p.getBrideName())
                .weddingDate(p.getWeddingDate())
                .status(p.getStatus().name())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private ParticipantSummaryResponse toParticipantSummary(ProjectParticipant p) {
        return ParticipantSummaryResponse.builder()
                .id(p.getId())
                .role(p.getRoleType().name())
                .roleType(p.getRoleType().name())
                .roleName("GROOM".equals(p.getRoleType().name()) ? "新郎" : "BRIDE".equals(p.getRoleType().name()) ? "新娘" : p.getRoleType().name())
                .status(p.getStatus().name())
                .currentBoardOrder(p.getCurrentBoardOrder())
                .joinedAt(p.getJoinedAt())
                .lastActiveAt(p.getLastActiveAt())
                .bound(p.getUserId() != null)
                .build();
    }

    private GeneratedContentSummaryResponse toContentSummary(GeneratedContent c) {
        return GeneratedContentSummaryResponse.builder()
                .id(c.getId())
                .contentType(c.getContentType().name())
                .contentTypeName(CONTENT_TYPE_NAMES.getOrDefault(c.getContentType(), c.getContentType().name()))
                .title(c.getTitle())
                .status(c.getStatus().name())
                .versionNo(c.getVersionNo())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
