package com.digitaldad.project.service;

import com.digitaldad.board.entity.ProjectBoard;
import com.digitaldad.board.repository.ProjectBoardRepository;
import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.project.dto.DeliverableDetailResponse;
import com.digitaldad.project.dto.GenerateDeliverableRequest;
import com.digitaldad.project.dto.UpdateDeliverableRequest;
import com.digitaldad.project.entity.GeneratedContent;
import com.digitaldad.project.entity.MaterialSnapshot;
import com.digitaldad.project.entity.Project;
import com.digitaldad.project.entity.ProjectParticipant;
import com.digitaldad.project.enums.ContentStatus;
import com.digitaldad.project.enums.ContentType;
import com.digitaldad.project.enums.ParticipantRole;
import com.digitaldad.project.repository.GeneratedContentRepository;
import com.digitaldad.project.repository.MaterialSnapshotRepository;
import com.digitaldad.project.repository.ProjectParticipantRepository;
import com.digitaldad.project.repository.ProjectRepository;
import com.digitaldad.prompt.dto.PromptContentDto;
import com.digitaldad.prompt.service.PromptSupplyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 交付物服务
 * <p>管理婚礼交付物（开场白、誓言等）的生成、查询、更新、删除及过期状态刷新。</p>
 */
@Service
@RequiredArgsConstructor
public class DeliverableService {

    private final ProjectRepository projectRepository;
    private final ProjectParticipantRepository participantRepository;
    private final GeneratedContentRepository contentRepository;
    private final MaterialSnapshotRepository snapshotRepository;
    private final ProjectBoardRepository projectBoardRepository;
    private final PromptSupplyService promptSupplyService;
    private final AiChatService aiChatService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Map<ContentType, String> CONTENT_TYPE_NAMES = Map.of(
            ContentType.OPENING_SPEECH, "婚礼开场白", ContentType.GROOM_VOW, "新郎誓言", ContentType.BRIDE_VOW, "新娘誓言");

    /**
     * 根据已确认素材生成交付物（AI 生成）
     */
    @Transactional
    public DeliverableDetailResponse generate(Long projectId, Long hostUserId, GenerateDeliverableRequest request) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new BusinessException(404, "项目不存在"));
        if (!project.getHostUserId().equals(hostUserId)) throw new BusinessException(403, "无权限访问该项目");
        ContentType contentType = request.getContentType();
        List<ProjectParticipant> participants = participantRepository.findByProjectIdOrderByRoleType(projectId);
        ProjectParticipant groom = participants.stream().filter(p -> p.getRoleType() == ParticipantRole.GROOM).findFirst().orElse(null);
        ProjectParticipant bride = participants.stream().filter(p -> p.getRoleType() == ParticipantRole.BRIDE).findFirst().orElse(null);

        List<MaterialSnapshot> snapshots;
        Long participantId;
        switch (contentType) {
            case OPENING_SPEECH -> {
                if (groom == null || bride == null) throw new BusinessException(400, "需要新郎和新娘都完成素材确认");
                List<MaterialSnapshot> groomSnap = snapshotRepository.findByProjectIdAndParticipantIdOrderByProjectBoardIdAsc(projectId, groom.getId());
                List<MaterialSnapshot> brideSnap = snapshotRepository.findByProjectIdAndParticipantIdOrderByProjectBoardIdAsc(projectId, bride.getId());
                if (groomSnap.isEmpty()) throw new BusinessException(400, "新郎暂无已确认素材");
                if (brideSnap.isEmpty()) throw new BusinessException(400, "新娘暂无已确认素材");
                snapshots = mergeSnapshotsByBoard(groomSnap, brideSnap);
                participantId = null;
            }
            case GROOM_VOW -> {
                if (groom == null) throw new BusinessException(400, "项目暂无新郎参与者");
                snapshots = snapshotRepository.findByProjectIdAndParticipantIdOrderByProjectBoardIdAsc(projectId, groom.getId());
                if (snapshots.isEmpty()) throw new BusinessException(400, "新郎暂无已确认素材");
                participantId = groom.getId();
            }
            case BRIDE_VOW -> {
                if (bride == null) throw new BusinessException(400, "项目暂无新娘参与者");
                snapshots = snapshotRepository.findByProjectIdAndParticipantIdOrderByProjectBoardIdAsc(projectId, bride.getId());
                if (snapshots.isEmpty()) throw new BusinessException(400, "新娘暂无已确认素材");
                participantId = bride.getId();
            }
            default -> throw new BusinessException(400, "不支持的交付物类型");
        }

        String materialsJson = buildMaterialsJson(snapshots);
        String prompt = "";
        try {
            prompt = promptSupplyService.getDeliverablePrompts(contentType.name()).stream().map(PromptContentDto::getContent).collect(Collectors.joining("\n\n"));
        } catch (Exception ignored) {
            prompt = "请根据以下已确认素材，生成温暖、真挚的婚礼交付内容。";
        }
        String generated = aiChatService.generateDeliverable(prompt, materialsJson, contentType.name());
        LocalDateTime maxSnapshotAt = snapshots.stream().map(MaterialSnapshot::getCreatedAt).max(LocalDateTime::compareTo).orElse(null);

        var existing = contentRepository.findByProjectIdAndContentType(projectId, contentType);
        GeneratedContent content;
        if (existing.isPresent()) {
            content = existing.get();
            content.setContent(generated);
            content.setParticipantId(participantId);
            content.setVersionNo(content.getVersionNo() + 1);
            content.setStatus(ContentStatus.ACTIVE);
            content.setSnapshotVersionAt(maxSnapshotAt);
        } else {
            content = new GeneratedContent();
            content.setProjectId(projectId);
            content.setParticipantId(participantId);
            content.setContentType(contentType);
            content.setTitle(CONTENT_TYPE_NAMES.get(contentType));
            content.setContent(generated);
            content.setVersionNo(1);
            content.setStatus(ContentStatus.ACTIVE);
            content.setSnapshotVersionAt(maxSnapshotAt);
        }
        content = contentRepository.save(content);
        return toResponse(content);
    }

    /**
     * 按项目与内容类型获取交付物
     */
    public DeliverableDetailResponse getDetail(Long projectId, ContentType contentType, Long hostUserId) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new BusinessException(404, "项目不存在"));
        if (!project.getHostUserId().equals(hostUserId)) throw new BusinessException(403, "无权限");
        GeneratedContent content = contentRepository.findByProjectIdAndContentType(projectId, contentType).orElseThrow(() -> new BusinessException(404, "该类型交付物尚未生成"));
        refreshOutdatedStatus(content);
        return toResponse(content);
    }

    /**
     * 根据 ID 获取交付物
     */
    public DeliverableDetailResponse getById(Long id, Long hostUserId) {
        GeneratedContent content = contentRepository.findById(id).orElseThrow(() -> new BusinessException(404, "交付物不存在"));
        Project project = projectRepository.findById(content.getProjectId()).orElseThrow();
        if (!project.getHostUserId().equals(hostUserId)) throw new BusinessException(403, "无权限");
        refreshOutdatedStatus(content);
        return toResponse(content);
    }

    /**
     * 更新交付物内容
     */
    @Transactional
    public DeliverableDetailResponse update(Long id, Long hostUserId, UpdateDeliverableRequest request) {
        GeneratedContent content = contentRepository.findById(id).orElseThrow(() -> new BusinessException(404, "交付物不存在"));
        Project project = projectRepository.findById(content.getProjectId()).orElseThrow();
        if (!project.getHostUserId().equals(hostUserId)) throw new BusinessException(403, "无权限");
        if (request.getTitle() != null) content.setTitle(request.getTitle());
        if (request.getContent() != null) content.setContent(request.getContent());
        content.setVersionNo(content.getVersionNo() + 1);
        content = contentRepository.save(content);
        return toResponse(content);
    }

    /**
     * 删除交付物
     */
    @Transactional
    public void delete(Long id, Long hostUserId) {
        GeneratedContent content = contentRepository.findById(id).orElseThrow(() -> new BusinessException(404, "交付物不存在"));
        Project project = projectRepository.findById(content.getProjectId()).orElseThrow();
        if (!project.getHostUserId().equals(hostUserId)) throw new BusinessException(403, "无权限");
        contentRepository.delete(content);
    }

    /**
     * 刷新项目下所有交付物的过期状态（素材有更新时）
     */
    @Transactional
    public void refreshOutdatedStatusForProject(Long projectId) {
        contentRepository.findByProjectId(projectId).forEach(this::refreshOutdatedStatus);
    }

    private void refreshOutdatedStatus(GeneratedContent content) {
        if (content.getStatus() != ContentStatus.ACTIVE || content.getSnapshotVersionAt() == null) return;
        List<Long> participantIds = resolveParticipantIds(content);
        boolean hasNewer = snapshotRepository.findByProjectId(content.getProjectId()).stream()
                .filter(s -> participantIds.contains(s.getParticipantId()))
                .anyMatch(s -> s.getCreatedAt().isAfter(content.getSnapshotVersionAt()));
        if (hasNewer) {
            content.setStatus(ContentStatus.OUTDATED);
            contentRepository.save(content);
        }
    }

    private List<Long> resolveParticipantIds(GeneratedContent content) {
        return switch (content.getContentType()) {
            case OPENING_SPEECH -> participantRepository.findByProjectId(content.getProjectId()).stream().map(ProjectParticipant::getId).toList();
            case GROOM_VOW, BRIDE_VOW -> content.getParticipantId() != null ? List.of(content.getParticipantId()) : List.of();
            default -> List.of();
        };
    }

    private List<MaterialSnapshot> mergeSnapshotsByBoard(List<MaterialSnapshot> groomSnap, List<MaterialSnapshot> brideSnap) {
        List<MaterialSnapshot> merged = new ArrayList<>();
        Set<Long> boardIds = new HashSet<>();
        groomSnap.forEach(s -> boardIds.add(s.getProjectBoardId()));
        brideSnap.forEach(s -> boardIds.add(s.getProjectBoardId()));
        List<Long> orderedBoards = projectBoardRepository.findByProjectIdOrderByDisplayOrderAsc(groomSnap.isEmpty() ? brideSnap.get(0).getProjectId() : groomSnap.get(0).getProjectId()).stream().map(ProjectBoard::getId).toList();
        for (Long boardId : orderedBoards) {
            if (!boardIds.contains(boardId)) continue;
            groomSnap.stream().filter(s -> s.getProjectBoardId().equals(boardId)).findFirst().ifPresent(merged::add);
            brideSnap.stream().filter(s -> s.getProjectBoardId().equals(boardId)).findFirst().ifPresent(merged::add);
        }
        return merged;
    }

    private String buildMaterialsJson(List<MaterialSnapshot> snapshots) {
        List<JsonNode> all = new ArrayList<>();
        for (MaterialSnapshot s : snapshots) {
            try {
                JsonNode arr = objectMapper.readTree(s.getSnapshotPayload());
                if (arr.isArray()) for (JsonNode n : arr) all.add(n);
            } catch (Exception ignored) {}
        }
        try { return objectMapper.writeValueAsString(all); } catch (Exception e) { return "[]"; }
    }

    private DeliverableDetailResponse toResponse(GeneratedContent c) {
        return DeliverableDetailResponse.builder().id(c.getId()).projectId(c.getProjectId()).participantId(c.getParticipantId()).contentType(c.getContentType().name()).contentTypeName(CONTENT_TYPE_NAMES.get(c.getContentType())).versionNo(c.getVersionNo()).title(c.getTitle()).content(c.getContent()).status(c.getStatus().name()).snapshotVersionAt(c.getSnapshotVersionAt()).createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt()).build();
    }
}
