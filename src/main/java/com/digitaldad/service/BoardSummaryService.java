package com.digitaldad.project.service;

import com.digitaldad.board.entity.BoardMeta;
import com.digitaldad.board.entity.ProjectBoard;
import com.digitaldad.board.repository.BoardMetaRepository;
import com.digitaldad.board.repository.ProjectBoardRepository;
import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.project.dto.*;
import com.digitaldad.project.entity.BoardSummary;
import com.digitaldad.project.entity.MaterialSnapshot;
import com.digitaldad.project.entity.ProjectParticipant;
import com.digitaldad.project.entity.SummaryItem;
import com.digitaldad.project.enums.ParticipantStatus;
import com.digitaldad.project.enums.SessionStatus;
import com.digitaldad.project.enums.SummaryItemType;
import com.digitaldad.project.enums.SummaryStatus;
import com.digitaldad.project.repository.*;
import com.digitaldad.prompt.dto.PromptContentDto;
import com.digitaldad.prompt.service.PromptSupplyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 板块小结服务
 * <p>管理对话小结的 AI 生成、条目的增删改、确认等。</p>
 */
@Service
@RequiredArgsConstructor
public class BoardSummaryService {

    private final BoardSummaryRepository summaryRepository;
    private final SummaryItemRepository itemRepository;
    private final ConversationMessageRepository messageRepository;
    private final InterviewSessionRepository sessionRepository;
    private final MaterialSnapshotRepository snapshotRepository;
    private final ProjectParticipantRepository participantRepository;
    private final ProjectBoardRepository projectBoardRepository;
    private final BoardMetaRepository boardMetaRepository;
    private final PromptSupplyService promptSupplyService;
    private final AiChatService aiChatService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private void checkSessionAccess(Long sessionId, Long userId) {
        var session = sessionRepository.findById(sessionId).orElseThrow(() -> new BusinessException(404, "会话不存在"));
        var p = participantRepository.findById(session.getParticipantId()).orElseThrow();
        if (!p.getUserId().equals(userId)) throw new BusinessException(403, "无权限");
    }

    /**
     * 创建小结（AI 根据对话生成）
     */
    @Transactional
    public BoardSummaryResponse createSummary(Long sessionId, Long userId) {
        checkSessionAccess(sessionId, userId);
        var session = sessionRepository.findById(sessionId).orElseThrow(() -> new BusinessException(404, "会话不存在"));
        var participant = participantRepository.findById(session.getParticipantId()).orElseThrow();
        var projectBoard = session.getCurrentProjectBoardId() != null ? projectBoardRepository.findById(session.getCurrentProjectBoardId()).orElse(null) : null;
        if (projectBoard == null) throw new BusinessException(400, "当前无板块");

        var existing = summaryRepository.findBySessionIdAndProjectBoardId(sessionId, projectBoard.getId());
        if (existing.isPresent() && existing.get().getStatus() != SummaryStatus.DRAFT) {
            return toSummaryResponse(existing.get());
        }

        String conversation = messageRepository.findBySessionIdOrderBySequenceNoAsc(sessionId).stream()
                .filter(m -> m.getContent() != null && !m.getContent().isBlank())
                .map(m -> (m.getSenderType().name().equals("USER") ? "用户：" : "AI：") + m.getContent())
                .collect(Collectors.joining("\n"));

        String boardCode = boardMetaRepository.findById(projectBoard.getBoardMetaId()).map(BoardMeta::getCode).orElse("COMMON");
        String boardName = boardMetaRepository.findById(projectBoard.getBoardMetaId()).map(BoardMeta::getName).orElse("板块");

        String prompt = "";
        try {
            var prompts = promptSupplyService.getSummaryPrompts(boardCode);
            prompt = prompts.stream().map(PromptContentDto::getContent).collect(Collectors.joining("\n\n"));
        } catch (Exception ignored) {
            prompt = "请将对话整理成结构化小结。";
        }
        String json = aiChatService.generateSummary(prompt, conversation, boardName);

        BoardSummary summary = existing.orElse(new BoardSummary());
        summary.setSessionId(sessionId);
        summary.setParticipantId(participant.getId());
        summary.setProjectId(session.getProjectId());
        summary.setProjectBoardId(projectBoard.getId());
        summary.setStatus(SummaryStatus.GENERATED);
        summary.setTitle(boardName + "小结");
        summary.setContentJson(json);
        summary.setGeneratedAt(LocalDateTime.now());
        if (existing.isEmpty()) summary.setVersionNo(1);
        summary = summaryRepository.save(summary);

        parseAndSaveItems(summary.getId(), json);
        summary.setStatus(SummaryStatus.WAITING_CONFIRM);
        summaryRepository.save(summary);
        return toSummaryResponse(summary);
    }

    private void parseAndSaveItems(Long summaryId, String json) {
        itemRepository.findBySummaryIdOrderByItemOrderAsc(summaryId).forEach(itemRepository::delete);
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode core = root.path("core_points");
            int order = 0;
            if (core.isArray()) {
                for (JsonNode n : core) {
                    SummaryItem item = new SummaryItem();
                    item.setSummaryId(summaryId);
                    item.setItemType("表达类".equals(n.path("type").asText()) ? SummaryItemType.EXPRESSION : SummaryItemType.FACT);
                    item.setContent(n.path("content").asText());
                    item.setItemOrder(order++);
                    item.setIsSelected(true);
                    itemRepository.save(item);
                }
            }
        } catch (Exception e) {
            SummaryItem fallback = new SummaryItem();
            fallback.setSummaryId(summaryId);
            fallback.setItemType(SummaryItemType.FACT);
            fallback.setContent("[解析失败，请手动编辑]");
            fallback.setItemOrder(0);
            fallback.setIsSelected(true);
            itemRepository.save(fallback);
        }
    }

    /**
     * 根据 ID 获取小结详情
     */
    public BoardSummaryResponse getSummary(Long summaryId, Long userId) {
        var summary = summaryRepository.findById(summaryId).orElseThrow(() -> new BusinessException(404, "小结不存在"));
        checkSessionAccess(summary.getSessionId(), userId);
        return toSummaryResponse(summary);
    }

    /**
     * 获取当前板块的小结（若无则返回 null）
     */
    public BoardSummaryResponse getCurrentSummary(Long sessionId, Long userId) {
        checkSessionAccess(sessionId, userId);
        var session = sessionRepository.findById(sessionId).orElseThrow();
        if (session.getCurrentProjectBoardId() == null) return null;
        var opt = summaryRepository.findBySessionIdAndProjectBoardId(sessionId, session.getCurrentProjectBoardId());
        return opt.map(this::toSummaryResponse).orElse(null);
    }

    /**
     * 更新小结条目
     */
    @Transactional
    public SummaryItemResponse updateItem(Long itemId, Long userId, UpdateSummaryItemRequest request) {
        var item = itemRepository.findById(itemId).orElseThrow(() -> new BusinessException(404, "条目不存在"));
        checkSessionAccess(summaryRepository.findById(item.getSummaryId()).orElseThrow().getSessionId(), userId);
        if (request.getContent() != null) item.setContent(request.getContent());
        if (request.getItemType() != null) item.setItemType(SummaryItemType.valueOf(request.getItemType().toUpperCase()));
        if (request.getIsSelected() != null) item.setIsSelected(request.getIsSelected());
        item = itemRepository.save(item);
        return toItemResponse(item);
    }

    /**
     * 在小结中新增条目
     */
    @Transactional
    public SummaryItemResponse addItem(Long summaryId, Long userId, AddSummaryItemRequest request) {
        var summary = summaryRepository.findById(summaryId).orElseThrow(() -> new BusinessException(404, "小结不存在"));
        checkSessionAccess(summary.getSessionId(), userId);
        if (summary.getStatus() == SummaryStatus.CONFIRMED) throw new BusinessException(400, "已确认的小结不可新增条目");
        int maxOrder = itemRepository.findBySummaryIdOrderByItemOrderAsc(summaryId).stream().mapToInt(SummaryItem::getItemOrder).max().orElse(-1) + 1;
        SummaryItem item = new SummaryItem();
        item.setSummaryId(summaryId);
        item.setItemType(SummaryItemType.valueOf(request.getItemType().toUpperCase()));
        item.setContent(request.getContent());
        item.setItemOrder(maxOrder);
        item.setIsSelected(true);
        item = itemRepository.save(item);
        return toItemResponse(item);
    }

    /**
     * 删除小结条目
     */
    @Transactional
    public void deleteItem(Long itemId, Long userId) {
        var item = itemRepository.findById(itemId).orElseThrow(() -> new BusinessException(404, "条目不存在"));
        var summary = summaryRepository.findById(item.getSummaryId()).orElseThrow();
        checkSessionAccess(summary.getSessionId(), userId);
        if (summary.getStatus() == SummaryStatus.CONFIRMED) throw new BusinessException(400, "已确认的小结不可删除条目");
        itemRepository.delete(item);
    }

    /**
     * 确认小结，将勾选条目写入素材快照，并推进参与者的板块进度
     */
    @Transactional
    public void confirmSummary(Long summaryId, Long userId) {
        var summary = summaryRepository.findById(summaryId).orElseThrow(() -> new BusinessException(404, "小结不存在"));
        checkSessionAccess(summary.getSessionId(), userId);
        if (summary.getStatus() == SummaryStatus.CONFIRMED) throw new BusinessException(400, "小结已确认");

        var selected = itemRepository.findBySummaryIdAndIsSelectedTrueOrderByItemOrderAsc(summaryId);
        if (selected.isEmpty()) throw new BusinessException(400, "请至少勾选一条内容");

        List<Map<String, Object>> payload = selected.stream()
                .map(i -> Map.<String, Object>of("content", i.getContent(), "type", i.getItemType().name()))
                .collect(Collectors.toList());
        try {
            String json = objectMapper.writeValueAsString(payload);
            MaterialSnapshot snap = new MaterialSnapshot();
            snap.setProjectId(summary.getProjectId());
            snap.setParticipantId(summary.getParticipantId());
            snap.setProjectBoardId(summary.getProjectBoardId());
            snap.setSummaryId(summaryId);
            snap.setSnapshotPayload(json);
            snapshotRepository.save(snap);
        } catch (Exception e) {
            throw new BusinessException(500, "快照创建失败");
        }

        summary.setStatus(SummaryStatus.CONFIRMED);
        summary.setConfirmedAt(LocalDateTime.now());
        summaryRepository.save(summary);

        var participant = participantRepository.findById(summary.getParticipantId()).orElseThrow();
        var boards = projectBoardRepository.findByProjectIdOrderByDisplayOrderAsc(summary.getProjectId());
        var currentBoard = projectBoardRepository.findById(summary.getProjectBoardId()).orElse(null);
        if (currentBoard != null) {
            int nextOrder = currentBoard.getDisplayOrder() + 1;
            participant.setCurrentBoardOrder(nextOrder);
            var nextBoard = boards.stream().filter(b -> b.getDisplayOrder() >= nextOrder).findFirst();
            if (nextBoard.isEmpty()) participant.setStatus(ParticipantStatus.COMPLETED);
            participantRepository.save(participant);
        }

        var session = sessionRepository.findByParticipantIdAndStatusIn(summary.getParticipantId(), List.of(SessionStatus.ACTIVE, SessionStatus.WAITING_CONFIRM)).orElse(null);
        if (session != null) {
            var nextBoard = boards.stream().filter(b -> b.getDisplayOrder() > (currentBoard != null ? currentBoard.getDisplayOrder() : -1)).findFirst();
            session.setCurrentProjectBoardId(nextBoard.map(ProjectBoard::getId).orElse(null));
            session.setStatus(nextBoard.isEmpty() ? SessionStatus.COMPLETED : SessionStatus.ACTIVE);
            sessionRepository.save(session);
        }
    }

    private BoardSummaryResponse toSummaryResponse(BoardSummary s) {
        var items = itemRepository.findBySummaryIdOrderByItemOrderAsc(s.getId()).stream().map(this::toItemResponse).collect(Collectors.toList());
        String boardCode = null, boardName = null;
        var pbOpt = projectBoardRepository.findById(s.getProjectBoardId());
        if (pbOpt.isPresent()) {
            var bmOpt = boardMetaRepository.findById(pbOpt.get().getBoardMetaId());
            if (bmOpt.isPresent()) { boardCode = bmOpt.get().getCode(); boardName = bmOpt.get().getName(); }
        }
        return BoardSummaryResponse.builder().id(s.getId()).sessionId(s.getSessionId()).projectBoardId(s.getProjectBoardId()).boardCode(boardCode).boardName(boardName).versionNo(s.getVersionNo()).status(s.getStatus().name()).title(s.getTitle()).generatedAt(s.getGeneratedAt()).confirmedAt(s.getConfirmedAt()).items(items).build();
    }

    private SummaryItemResponse toItemResponse(SummaryItem i) {
        return SummaryItemResponse.builder().id(i.getId()).summaryId(i.getSummaryId()).itemType(i.getItemType().name()).content(i.getContent()).itemOrder(i.getItemOrder()).isSelected(i.getIsSelected()).createdAt(i.getCreatedAt()).build();
    }
}
