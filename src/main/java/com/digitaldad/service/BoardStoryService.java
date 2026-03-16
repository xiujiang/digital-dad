package com.digitaldad.service;

import com.digitaldad.entity.BoardMeta;
import com.digitaldad.entity.ProjectBoard;
import com.digitaldad.repository.BoardMetaRepository;
import com.digitaldad.repository.ProjectBoardRepository;
import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.dto.BoardStoryResponse;
import com.digitaldad.dto.UserBoardItemDto;
import com.digitaldad.entity.BoardStory;
import com.digitaldad.repository.BoardStoryRepository;
import com.digitaldad.repository.BoardSummaryRepository;
import com.digitaldad.repository.InterviewSessionRepository;
import com.digitaldad.repository.SummaryItemRepository;
import com.digitaldad.repository.KeyPersonRepository;
import com.digitaldad.repository.ProjectParticipantRepository;
import com.digitaldad.dto.PromptContentDto;
import com.digitaldad.entity.BoardSummary;
import com.digitaldad.entity.SummaryItem;
import com.digitaldad.service.PromptSupplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 故事/时光服务
 * <p>根据板块小结 AI 生成板块故事，用于展示温暖的故事叙述。故事与小结形成递进：消息→小结→故事。</p>
 */
@Service
@RequiredArgsConstructor
public class BoardStoryService {

    private final BoardStoryRepository storyRepository;
    private final InterviewSessionRepository sessionRepository;
    private final BoardSummaryRepository summaryRepository;
    private final SummaryItemRepository summaryItemRepository;
    private final ProjectBoardRepository projectBoardRepository;
    private final BoardMetaRepository boardMetaRepository;
    private final PromptSupplyService promptSupplyService;
    private final AiChatService aiChatService;
    private final ProjectParticipantRepository participantRepository;
    private final KeyPersonRepository keyPersonRepository;

    private void checkSessionAccess(Long sessionId, Long userId) {
        var session = sessionRepository.findById(sessionId).orElseThrow(() -> new BusinessException(404, "会话不存在"));
        var p = participantRepository.findById(session.getParticipantId()).orElseThrow();
        if (!p.getUserId().equals(userId)) throw new BusinessException(403, "无权限");
    }

    /**
     * 创建/生成板块故事（基于本板块小结，形成 消息→小结→故事 的递进）
     */
    @Transactional
    public BoardStoryResponse createStory(Long sessionId, Long userId) {
        checkSessionAccess(sessionId, userId);
        var session = sessionRepository.findById(sessionId).orElseThrow(() -> new BusinessException(404, "会话不存在"));
        var projectBoard = session.getCurrentProjectBoardId() != null ? projectBoardRepository.findById(session.getCurrentProjectBoardId()).orElse(null) : null;
        if (projectBoard == null) throw new BusinessException(400, "当前无板块");

        BoardSummary summary = summaryRepository.findBySessionIdAndProjectBoardId(sessionId, projectBoard.getId())
                .orElseThrow(() -> new BusinessException(400, "请先完成本板块小结"));

        String summaryContent = buildSummaryContent(summary);
        if (summaryContent.isBlank()) throw new BusinessException(400, "小结暂无有效内容，请先完善小结后再生成故事");

        String boardCode = boardMetaRepository.findById(projectBoard.getBoardMetaId()).map(BoardMeta::getCode).orElse("COMMON");
        String boardName = boardMetaRepository.findById(projectBoard.getBoardMetaId()).map(BoardMeta::getName).orElse("板块");

        String prompt = "";
        try {
            var prompts = promptSupplyService.getStoryPrompts(boardCode);
            prompt = prompts.stream().map(PromptContentDto::getContent).collect(Collectors.joining("\n\n"));
        } catch (Exception ignored) {
            prompt = "请将以下小结整理成一段温暖的故事叙述。";
        }
        String content = aiChatService.generateStory(prompt, summaryContent, boardName);

        var existing = storyRepository.findBySessionIdAndProjectBoardId(sessionId, projectBoard.getId());
        BoardStory story = existing.orElse(new BoardStory());
        story.setSessionId(sessionId);
        story.setParticipantId(session.getParticipantId());
        story.setProjectId(session.getProjectId());
        story.setProjectBoardId(projectBoard.getId());
        story.setContent(content);
        story.setVersionNo(existing.isPresent() ? story.getVersionNo() + 1 : 1);
        story = storyRepository.save(story);
        return toResponse(story);
    }

    /**
     * 将小结拼成一段文本（标题 + 条目内容，优先已勾选条目）
     */
    private String buildSummaryContent(BoardSummary summary) {
        List<SummaryItem> items = summaryItemRepository.findBySummaryIdAndIsSelectedTrueOrderByItemOrderAsc(summary.getId());
        if (items.isEmpty()) {
            items = summaryItemRepository.findBySummaryIdOrderByItemOrderAsc(summary.getId());
        }
        StringBuilder sb = new StringBuilder();
        if (summary.getTitle() != null && !summary.getTitle().isBlank()) {
            sb.append(summary.getTitle()).append("\n\n");
        }
        items.stream()
                .map(SummaryItem::getContent)
                .filter(c -> c != null && !c.isBlank())
                .forEach(c -> sb.append(c).append("\n"));
        return sb.length() > 0 ? sb.toString().trim() : "";
    }

    /**
     * 获取指定板块的故事
     */
    public BoardStoryResponse getStory(Long sessionId, Long projectBoardId, Long userId) {
        checkSessionAccess(sessionId, userId);
        return storyRepository.findBySessionIdAndProjectBoardId(sessionId, projectBoardId).map(this::toResponse).orElse(null);
    }

    /**
     * C 端：当前用户有故事的项目板块列表（用于按板块展示故事时的 Tab/筛选）。
     * <p>数据来自 board_story，仅返回用户有故事记录的板块；查某板块故事时用返回的 projectBoardId 调 GET /api/c/users/me/stories?projectBoardId=xxx。</p>
     *
     * @param userId 当前用户 ID
     * @return 板块列表，按 projectId、displayOrder 排序
     */
    public List<UserBoardItemDto> listBoardsWithStories(Long userId) {
        List<Long> participantIds = participantRepository.findByUserId(userId).stream()
                .map(com.digitaldad.entity.ProjectParticipant::getId)
                .collect(Collectors.toList());
        if (participantIds.isEmpty()) return List.of();

        List<Long> projectBoardIds = storyRepository.findDistinctProjectBoardIdsByParticipantIdIn(participantIds);
        if (projectBoardIds.isEmpty()) return List.of();

        List<ProjectBoard> boards = projectBoardRepository.findAllById(projectBoardIds);
        Set<Long> metaIds = boards.stream().map(ProjectBoard::getBoardMetaId).collect(Collectors.toSet());
        var metaMap = boardMetaRepository.findAllById(metaIds).stream().collect(Collectors.toMap(BoardMeta::getId, bm -> bm));

        return boards.stream()
                .map(pb -> {
                    BoardMeta meta = metaMap.get(pb.getBoardMetaId());
                    return UserBoardItemDto.builder()
                            .projectId(pb.getProjectId())
                            .projectBoardId(pb.getId())
                            .boardCode(meta != null ? meta.getCode() : null)
                            .boardName(meta != null ? meta.getName() : null)
                            .displayOrder(pb.getDisplayOrder() != null ? pb.getDisplayOrder() : 0)
                            .build();
                })
                .sorted(Comparator.comparing(UserBoardItemDto::getProjectId, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(UserBoardItemDto::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
    }

    /**
     * C 端：当前用户查询自己的全部故事，支持按会话、板块、角色筛选，分页返回。
     *
     * @param userId         当前用户 ID
     * @param sessionId      可选，限定某次会话
     * @param projectBoardId 可选，限定某项目板块
     * @param boardCode      可选，限定板块类型编码
     * @param roleLabel      可选，限定包含该角色的会话下的故事（关键人物 roleLabel）
     * @param page           页码，从 1 开始
     * @param size           每页条数
     * @return 分页结果，按创建时间倒序
     */
    public Page<BoardStoryResponse> listMyStories(Long userId, Long sessionId, Long projectBoardId, String boardCode, String roleLabel, int page, int size) {
        List<Long> participantIds = participantRepository.findByUserId(userId).stream()
                .map(com.digitaldad.entity.ProjectParticipant::getId)
                .collect(Collectors.toList());
        if (participantIds.isEmpty()) return new PageImpl<>(List.of(), PageRequest.of(0, size), 0);

        List<BoardStory> stories = storyRepository.findByParticipantIdIn(participantIds, Sort.by(Sort.Direction.DESC, "createdAt"));

        Set<Long> sessionIdsForRole = null;
        if (roleLabel != null && !roleLabel.isBlank()) {
            sessionIdsForRole = keyPersonRepository.findByUserIdAndRoleLabel(userId, roleLabel.trim()).stream()
                    .map(com.digitaldad.entity.KeyPerson::getSessionId)
                    .filter(sid -> sid != null)
                    .collect(Collectors.toSet());
            if (sessionIdsForRole.isEmpty()) return new PageImpl<>(List.of(), PageRequest.of(0, size), 0);
        }

        final Set<Long> finalSessionIdsForRole = sessionIdsForRole;
        List<BoardStoryResponse> all = stories.stream()
                .filter(s -> sessionId == null || s.getSessionId().equals(sessionId))
                .filter(s -> projectBoardId == null || s.getProjectBoardId().equals(projectBoardId))
                .filter(s -> finalSessionIdsForRole == null || finalSessionIdsForRole.contains(s.getSessionId()))
                .filter(s -> {
                    if (boardCode == null || boardCode.isBlank()) return true;
                    var pb = projectBoardRepository.findById(s.getProjectBoardId());
                    if (pb.isEmpty()) return false;
                    return boardMetaRepository.findById(pb.get().getBoardMetaId())
                            .map(bm -> boardCode.trim().equalsIgnoreCase(bm.getCode()))
                            .orElse(false);
                })
                .map(this::toResponse)
                .collect(Collectors.toList());

        int total = all.size();
        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.min(50, Math.max(1, size));
        int from = Math.min(pageIndex * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<BoardStoryResponse> content = from < total ? all.subList(from, to) : List.of();
        return new PageImpl<>(content, PageRequest.of(pageIndex, pageSize), total);
    }

    private BoardStoryResponse toResponse(BoardStory s) {
        String boardCode = null, boardName = null;
        var pbOpt = projectBoardRepository.findById(s.getProjectBoardId());
        if (pbOpt.isPresent()) {
            var bmOpt = boardMetaRepository.findById(pbOpt.get().getBoardMetaId());
            if (bmOpt.isPresent()) { boardCode = bmOpt.get().getCode(); boardName = bmOpt.get().getName(); }
        }
        return BoardStoryResponse.builder().id(s.getId()).sessionId(s.getSessionId()).projectId(s.getProjectId()).projectBoardId(s.getProjectBoardId()).boardCode(boardCode).boardName(boardName).content(s.getContent()).versionNo(s.getVersionNo()).createdAt(s.getCreatedAt()).build();
    }
}
