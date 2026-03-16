package com.digitaldad.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.dto.ProjectInfoResponse;
import com.digitaldad.dto.ProjectDetailResponse;
import com.digitaldad.dto.ProjectListItemResponse;
import com.digitaldad.dto.ShareEntryResponse;
import com.digitaldad.dto.AdminProjectListItemResponse;
import com.digitaldad.dto.CreateProjectRequest;
import com.digitaldad.dto.UpdateProjectRequest;
import com.digitaldad.dto.EntryRoleOption;
import com.digitaldad.dto.EntryHostInfo;
import com.digitaldad.dto.AdminProjectListRequest;
import com.digitaldad.dto.AdminProjectDetailResponse;
import com.digitaldad.dto.ParticipantBoardMaterialResponse;
import com.digitaldad.dto.ParticipantBoardStoryResponse;
import com.digitaldad.dto.ParticipantSummaryResponse;
import com.digitaldad.dto.GeneratedContentSummaryResponse;
import com.digitaldad.entity.BoardMeta;
import com.digitaldad.entity.BoardStory;
import com.digitaldad.entity.GeneratedContent;
import com.digitaldad.entity.MaterialSnapshot;
import com.digitaldad.entity.Project;
import com.digitaldad.entity.ProjectBoard;
import com.digitaldad.entity.ProjectParticipant;
import com.digitaldad.enums.ContentStatus;
import com.digitaldad.enums.ContentType;
import com.digitaldad.enums.ParticipantRole;
import com.digitaldad.enums.ParticipantStatus;
import com.digitaldad.enums.ProjectStatus;
import com.digitaldad.repository.BoardMetaRepository;
import com.digitaldad.repository.BoardStoryRepository;
import com.digitaldad.repository.GeneratedContentRepository;
import com.digitaldad.repository.MaterialSnapshotRepository;
import com.digitaldad.repository.ProjectBoardRepository;
import com.digitaldad.repository.ProjectParticipantRepository;
import com.digitaldad.repository.ProjectRepository;
import com.digitaldad.service.ProjectBoardService;
import com.digitaldad.service.WechatSchemeService;
import com.digitaldad.entity.User;
import com.digitaldad.enums.QuotaType;
import com.digitaldad.repository.UserRepository;
import com.digitaldad.enums.RefType;
import com.digitaldad.service.UserQuotaService;
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
import java.util.ArrayList;
import java.util.HashMap;
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
    private final MaterialSnapshotRepository snapshotRepository;
    private final BoardStoryRepository storyRepository;
    private final ProjectBoardRepository projectBoardRepository;
    private final ProjectBoardService projectBoardService;
    private final BoardMetaRepository boardMetaRepository;
    private final DeliverableService deliverableService;
    private final UserQuotaService userQuotaService;
    private final UserRepository userRepository;
    private final WechatSchemeService wechatSchemeService;

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
        project.setTheme(request.getTheme());
        project.setContactInfo(request.getContactInfo());
        project.setShareToken(shareToken);
        project = projectRepository.save(project);

        projectBoardService.ensureDefaultBoardsForNewProject(project.getId());

        return getProjectDetail(project.getId(), hostUserId);
    }

    /**
     * 更新项目（主持人仅能更新自己的项目，仅更新请求中非 null 的字段）
     */
    @Transactional
    public ProjectDetailResponse updateProject(Long projectId, Long hostUserId, UpdateProjectRequest request) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(404, "项目不存在"));
        if (!project.getHostUserId().equals(hostUserId)) {
            throw new BusinessException(403, "无权限修改该项目");
        }
        if (request.getGroomName() != null) project.setGroomName(request.getGroomName());
        if (request.getBrideName() != null) project.setBrideName(request.getBrideName());
        if (request.getWeddingDate() != null) project.setWeddingDate(request.getWeddingDate());
        if (request.getTheme() != null) project.setTheme(request.getTheme());
        if (request.getContactInfo() != null) project.setContactInfo(request.getContactInfo());
        projectRepository.save(project);
        return getProjectDetail(projectId, hostUserId);
    }

    /**
     * 更新项目（超管可更新任意项目）
     */
    @Transactional
    public AdminProjectDetailResponse updateProjectForAdmin(Long projectId, UpdateProjectRequest request) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(404, "项目不存在"));
        if (request.getGroomName() != null) project.setGroomName(request.getGroomName());
        if (request.getBrideName() != null) project.setBrideName(request.getBrideName());
        if (request.getWeddingDate() != null) project.setWeddingDate(request.getWeddingDate());
        if (request.getTheme() != null) project.setTheme(request.getTheme());
        if (request.getContactInfo() != null) project.setContactInfo(request.getContactInfo());
        projectRepository.save(project);
        return getProjectDetailForAdmin(projectId);
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
        Map<Long, List<ParticipantBoardMaterialResponse>> boardMaterialsByParticipant =
                buildBoardMaterialsByParticipant(projectId, participants);
        Map<Long, List<ParticipantBoardStoryResponse>> boardStoriesByParticipant =
                buildBoardStoriesByParticipant(projectId, participants);
        List<ParticipantSummaryResponse> participantSummaries = participants.stream()
                .map(p -> toParticipantSummary(p,
                        boardMaterialsByParticipant.getOrDefault(p.getId(), List.of()),
                        boardStoriesByParticipant.getOrDefault(p.getId(), List.of())))
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
                .theme(project.getTheme())
                .contactInfo(project.getContactInfo())
                .status(project.getStatus().name())
                .shareToken(project.getShareToken())
                .participants(participantSummaries)
                .contents(contentSummaries)
                .createdAt(project.getCreatedAt())
                .build();
    }

    /**
     * 分享入口（校验项目归属）
     */
    public ShareEntryResponse getShareEntry(Long projectId, Long hostUserId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(404, "项目不存在"));
        if (!project.getHostUserId().equals(hostUserId)) {
            throw new BusinessException(403, "无权限访问该项目");
        }
        return buildShareEntryResponse(project);
    }

    /**
     * 分享入口（超管可查任意项目）
     */
    public ShareEntryResponse getShareEntryForAdmin(Long projectId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(404, "项目不存在"));
        return buildShareEntryResponse(project);
    }

    private ShareEntryResponse buildShareEntryResponse(Project project) {
        String entryUrl = baseUrl + "/c/entry/" + project.getShareToken();
        String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + URLEncoder.encode(entryUrl, StandardCharsets.UTF_8);
        ShareEntryResponse.ShareEntryResponseBuilder builder = ShareEntryResponse.builder()
                .projectId(project.getId())
                .shareToken(project.getShareToken())
                .entryUrl(entryUrl)
                .qrCodeUrl(qrCodeUrl);
        wechatSchemeService.generateSchemeForShareToken(project.getShareToken()).ifPresent(scheme -> {
            builder.wechatScheme(scheme);
            builder.wechatSchemeQrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + URLEncoder.encode(scheme, StandardCharsets.UTF_8));
        });
        return builder.build();
    }

    /**
     * 根据 share_token 获取项目信息（C 端扫码进入用）
     * <p>含 roleOptions：可选角色由后端返回（新郎/新娘及是否已被占用），前端按此渲染选择身份页。</p>
     */
    public ProjectInfoResponse getProjectInfoByToken(String shareToken) {
        Project project = projectRepository.findByShareTokenAndDeletedAtIsNull(shareToken)
                .orElseThrow(() -> new BusinessException(404, "链接无效或已过期"));
        List<EntryRoleOption> roleOptions = List.of(
                EntryRoleOption.builder()
                        .role(ParticipantRole.GROOM.name())
                        .label("新郎")
                        .available(!participantRepository.existsByProjectIdAndRoleType(project.getId(), ParticipantRole.GROOM))
                        .build(),
                EntryRoleOption.builder()
                        .role(ParticipantRole.BRIDE.name())
                        .label("新娘")
                        .available(!participantRepository.existsByProjectIdAndRoleType(project.getId(), ParticipantRole.BRIDE))
                        .build()
        );
        // 主持人信息：姓名来自用户；联系方式优先用项目上的 contactInfo，没有再用主持人手机号
        EntryHostInfo hostInfo = null;
        User host = userRepository.findById(project.getHostUserId()).orElse(null);
        if (host != null) {
            String contact = (project.getContactInfo() != null && !project.getContactInfo().isBlank())
                    ? project.getContactInfo().trim()
                    : host.getPhone();
            hostInfo = EntryHostInfo.builder()
                    .name(host.getName() != null && !host.getName().isBlank() ? host.getName() : host.getPhone())
                    .phone(contact)
                    .build();
        }
        return ProjectInfoResponse.builder()
                .projectId(project.getId())
                .groomName(project.getGroomName())
                .brideName(project.getBrideName())
                .weddingDate(project.getWeddingDate())
                .theme(project.getTheme())
                .shareToken(shareToken)
                .roleOptions(roleOptions)
                .host(hostInfo)
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

        com.digitaldad.enums.ProjectStatus statusEnum = null;
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                statusEnum = com.digitaldad.enums.ProjectStatus.valueOf(request.getStatus().trim().toUpperCase());
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
        Map<Long, List<ParticipantBoardMaterialResponse>> boardMaterialsByParticipant =
                buildBoardMaterialsByParticipant(projectId, participants);
        Map<Long, List<ParticipantBoardStoryResponse>> boardStoriesByParticipant =
                buildBoardStoriesByParticipant(projectId, participants);
        List<ParticipantSummaryResponse> participantSummaries = participants.stream()
                .map(p -> toParticipantSummary(p,
                        boardMaterialsByParticipant.getOrDefault(p.getId(), List.of()),
                        boardStoriesByParticipant.getOrDefault(p.getId(), List.of())))
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
                .theme(project.getTheme())
                .contactInfo(project.getContactInfo())
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
                .theme(p.getTheme())
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
                .theme(p.getTheme())
                .status(p.getStatus().name())
                .createdAt(p.getCreatedAt())
                .build();
    }

    /**
     * 按参与者汇总：每个参与者在各板块下的最新素材快照（每个板块取 createdAt 最大的一条）
     */
    private Map<Long, List<ParticipantBoardMaterialResponse>> buildBoardMaterialsByParticipant(
            Long projectId, List<ProjectParticipant> participants) {
        List<ProjectBoard> boards = projectBoardRepository.findByProjectIdOrderByDisplayOrderAsc(projectId);
        if (boards.isEmpty()) {
            return participants.stream().collect(Collectors.toMap(ProjectParticipant::getId, p -> List.of()));
        }
        Map<Long, BoardMeta> metaMap = boardMetaRepository.findAllById(
                        boards.stream().map(ProjectBoard::getBoardMetaId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(BoardMeta::getId, m -> m));
        List<MaterialSnapshot> allSnapshots = snapshotRepository.findByProjectId(projectId);
        // participantId -> (projectBoardId -> 该板块下最新快照)
        Map<Long, Map<Long, MaterialSnapshot>> latestByParticipantAndBoard = new HashMap<>();
        for (MaterialSnapshot s : allSnapshots) {
            latestByParticipantAndBoard
                    .computeIfAbsent(s.getParticipantId(), k -> new HashMap<>())
                    .merge(s.getProjectBoardId(), s, (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b);
        }
        Map<Long, List<ParticipantBoardMaterialResponse>> result = new HashMap<>();
        for (ProjectParticipant p : participants) {
            Map<Long, MaterialSnapshot> byBoard = latestByParticipantAndBoard.getOrDefault(p.getId(), Map.of());
            List<ParticipantBoardMaterialResponse> list = new ArrayList<>();
            for (ProjectBoard board : boards) {
                MaterialSnapshot snap = byBoard.get(board.getId());
                if (snap != null) {
                    BoardMeta meta = metaMap.get(board.getBoardMetaId());
                    list.add(ParticipantBoardMaterialResponse.builder()
                            .projectBoardId(board.getId())
                            .boardCode(meta != null ? meta.getCode() : null)
                            .boardName(meta != null ? meta.getName() : null)
                            .displayOrder(board.getDisplayOrder())
                            .snapshotPayload(snap.getSnapshotPayload())
                            .snapshotCreatedAt(snap.getCreatedAt())
                            .summaryId(snap.getSummaryId())
                            .build());
                }
            }
            result.put(p.getId(), list);
        }
        return result;
    }

    /**
     * 按参与者汇总：每个参与者在各板块下的故事（每个板块取 createdAt 最新的一条）
     */
    private Map<Long, List<ParticipantBoardStoryResponse>> buildBoardStoriesByParticipant(
            Long projectId, List<ProjectParticipant> participants) {
        List<ProjectBoard> boards = projectBoardRepository.findByProjectIdOrderByDisplayOrderAsc(projectId);
        if (boards.isEmpty()) {
            return participants.stream().collect(Collectors.toMap(ProjectParticipant::getId, p -> List.of()));
        }
        Map<Long, BoardMeta> metaMap = boardMetaRepository.findAllById(
                        boards.stream().map(ProjectBoard::getBoardMetaId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(BoardMeta::getId, m -> m));
        Map<Long, List<ParticipantBoardStoryResponse>> result = new HashMap<>();
        for (ProjectParticipant p : participants) {
            List<BoardStory> stories = storyRepository.findByParticipantIdAndProjectId(p.getId(), projectId);
            // projectBoardId -> 该板块下最新故事（createdAt 最大）
            Map<Long, BoardStory> latestByBoard = new HashMap<>();
            for (BoardStory s : stories) {
                latestByBoard.merge(s.getProjectBoardId(), s,
                        (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b);
            }
            List<ParticipantBoardStoryResponse> list = new ArrayList<>();
            for (ProjectBoard board : boards) {
                BoardStory story = latestByBoard.get(board.getId());
                if (story != null) {
                    BoardMeta meta = metaMap.get(board.getBoardMetaId());
                    list.add(ParticipantBoardStoryResponse.builder()
                            .projectBoardId(board.getId())
                            .boardCode(meta != null ? meta.getCode() : null)
                            .boardName(meta != null ? meta.getName() : null)
                            .displayOrder(board.getDisplayOrder())
                            .id(story.getId())
                            .content(story.getContent())
                            .versionNo(story.getVersionNo())
                            .createdAt(story.getCreatedAt())
                            .build());
                }
            }
            result.put(p.getId(), list);
        }
        return result;
    }

    private ParticipantSummaryResponse toParticipantSummary(ProjectParticipant p,
                                                            List<ParticipantBoardMaterialResponse> boardMaterials,
                                                            List<ParticipantBoardStoryResponse> boardStories) {
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
                .boardMaterials(boardMaterials != null ? boardMaterials : List.of())
                .boardStories(boardStories != null ? boardStories : List.of())
                .build();
    }

    private GeneratedContentSummaryResponse toContentSummary(GeneratedContent c) {
        return GeneratedContentSummaryResponse.builder()
                .id(c.getId())
                .contentType(c.getContentType().name())
                .contentTypeName(CONTENT_TYPE_NAMES.getOrDefault(c.getContentType(), c.getContentType().name()))
                .title(c.getTitle())
                .status(c.getStatus().name())
                .usingLatestSnapshot(ContentStatus.ACTIVE.equals(c.getStatus()))
                .versionNo(c.getVersionNo())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
