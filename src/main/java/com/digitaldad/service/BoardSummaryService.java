package com.digitaldad.service;

import com.digitaldad.entity.BoardMeta;
import com.digitaldad.entity.BoardSummaryKeyPerson;
import com.digitaldad.entity.ProjectBoard;
import com.digitaldad.repository.BoardSummaryKeyPersonRepository;
import com.digitaldad.repository.BoardMetaRepository;
import com.digitaldad.repository.ProjectBoardRepository;
import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.dto.*;
import com.digitaldad.entity.BoardSummary;
import com.digitaldad.entity.KeyPerson;
import com.digitaldad.entity.MaterialSnapshot;
import com.digitaldad.entity.ProjectParticipant;
import com.digitaldad.entity.SummaryItem;
import com.digitaldad.enums.ParticipantStatus;
import com.digitaldad.enums.SessionStatus;
import com.digitaldad.enums.SummaryItemType;
import com.digitaldad.enums.SummaryStatus;
import com.digitaldad.repository.*;
import com.digitaldad.dto.PromptContentDto;
import com.digitaldad.service.PromptSupplyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(BoardSummaryService.class);
    private static final int LOG_SNIPPET_LEN = 800;

    private final BoardSummaryRepository summaryRepository;
    private final SummaryItemRepository itemRepository;
    private final BoardSummaryKeyPersonRepository summaryKeyPersonRepository;
    private final KeyPersonService keyPersonService;
    private final ConversationMessageRepository messageRepository;
    private final InterviewSessionRepository sessionRepository;
    private final MaterialSnapshotRepository snapshotRepository;
    private final ProjectParticipantRepository participantRepository;
    private final ProjectBoardRepository projectBoardRepository;
    private final BoardMetaRepository boardMetaRepository;
    private final PromptSupplyService promptSupplyService;
    private final AiChatService aiChatService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 不请求 AI 时使用的默认小结 JSON（仅用于 E2E/联调） */
    private static final String DEFAULT_TEST_SUMMARY_JSON = """
            {"title":"【测试】板块小结","core_points":[{"type":"事实类","content":"测试要点一：用户在本板块进行了对话。"},{"type":"表达类","content":"测试要点二：用于 E2E 流程验证。"}],"key_characters":[{"name":"测试人物","role_label":"家人"}]}
            """;

    private void checkSessionAccess(Long sessionId, Long userId) {
        var session = sessionRepository.findById(sessionId).orElseThrow(() -> new BusinessException(404, "会话不存在"));
        var p = participantRepository.findById(session.getParticipantId()).orElseThrow();
        if (!p.getUserId().equals(userId)) throw new BusinessException(403, "无权限");
    }

    /**
     * 创建小结（默认使用测试数据不请求 AI；useRealData 为 true 时才调用 AI 生成真实小结）
     *
     * @param useRealData true 时请求 AI 生成小结；false 时使用默认测试 JSON，用于 E2E/联调
     */
    @Transactional
    public BoardSummaryResponse createSummary(Long sessionId, Long userId, boolean useRealData) {
        checkSessionAccess(sessionId, userId);
        var session = sessionRepository.findById(sessionId).orElseThrow(() -> new BusinessException(404, "会话不存在"));
        var participant = participantRepository.findById(session.getParticipantId()).orElseThrow();
        var projectBoard = session.getCurrentProjectBoardId() != null ? projectBoardRepository.findById(session.getCurrentProjectBoardId()).orElse(null) : null;
        if (projectBoard == null) throw new BusinessException(400, "当前无板块");

        var existing = summaryRepository.findBySessionIdAndProjectBoardId(sessionId, projectBoard.getId());
        if (existing.isPresent() && existing.get().getStatus() != SummaryStatus.DRAFT) {
            return toSummaryResponse(existing.get());
        }

        String boardName = boardMetaRepository.findById(projectBoard.getBoardMetaId()).map(BoardMeta::getName).orElse("板块");

        String rawJson;
        if (!useRealData) {
            rawJson = DEFAULT_TEST_SUMMARY_JSON.trim();
            log.info("[小结] 使用默认测试数据，不请求 AI");
        } else {
            String conversation = messageRepository.findBySessionIdOrderBySequenceNoAsc(sessionId).stream()
                    .filter(m -> m.getContent() != null && !m.getContent().isBlank())
                    .map(m -> (m.getSenderType().name().equals("USER") ? "用户：" : "AI：") + m.getContent())
                    .collect(Collectors.joining("\n"));

            String boardCode = boardMetaRepository.findById(projectBoard.getBoardMetaId()).map(BoardMeta::getCode).orElse("COMMON");

            String prompt = "";
            try {
                var prompts = promptSupplyService.getSummaryPrompts(boardCode);
                prompt = prompts.stream().map(PromptContentDto::getContent).collect(Collectors.joining("\n\n"));
            } catch (Exception ignored) {
                prompt = "请将对话整理成结构化小结。";
            }
            rawJson = aiChatService.generateSummary(prompt, conversation, boardName);
            log.info("[小结] AI 原始返回长度={}, 前{}字: {}", rawJson == null ? 0 : rawJson.length(), LOG_SNIPPET_LEN, truncateForLog(rawJson, LOG_SNIPPET_LEN));
        }

        String json = normalizeSummaryJson(rawJson);
        log.info("[小结] 清洗后长度={}, 前{}字: {}", json.length(), LOG_SNIPPET_LEN, truncateForLog(json, LOG_SNIPPET_LEN));

        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("[小结] JSON 解析失败, sessionId={}, 异常: {} - {}, 失败内容前{}字: {}", sessionId, e.getClass().getSimpleName(), e.getMessage(), LOG_SNIPPET_LEN, truncateForLog(json, LOG_SNIPPET_LEN), e);
            throw new BusinessException(400, "AI 返回的小结格式无效，无法解析 JSON：" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }

        BoardSummary summary = existing.orElse(new BoardSummary());
        summary.setSessionId(sessionId);
        summary.setParticipantId(participant.getId());
        summary.setProjectId(session.getProjectId());
        summary.setProjectBoardId(projectBoard.getId());
        summary.setStatus(SummaryStatus.GENERATED);
        String aiTitle = root.path("title").asText(null);
        summary.setTitle(aiTitle != null && !aiTitle.isBlank() ? aiTitle : (boardName + "小结"));
        summary.setContentJson(json);
        summary.setGeneratedAt(LocalDateTime.now());
        if (existing.isEmpty()) summary.setVersionNo(1);
        summary = summaryRepository.save(summary);

        parseAndSaveItems(root, summary.getId());
        parseAndSaveKeyPersons(root, summary.getId(), participant.getUserId(), session.getId(), participant.getId());
        // 生成即确认：直接写入素材快照并推进板块进度
        applyConfirm(summary, userId);
        return toSummaryResponse(summary);
    }

    private static String truncateForLog(String s, int maxLen) {
        if (s == null) return "null";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...[截断,总长=" + s.length() + "]";
    }

    /** 去掉 AI 可能返回的 markdown 代码块包裹或前后叙述，只保留纯 JSON 字符串 */
    private String normalizeSummaryJson(String raw) {
        if (raw == null) {
            log.warn("[小结] AI 返回为 null，使用空 JSON");
            return "{}";
        }
        String s = raw.trim();
        if (s.startsWith("```")) {
            log.debug("[小结] 检测到 markdown 代码块，去除首尾 ```");
            int start = s.indexOf("\n");
            if (start > 0) s = s.substring(start + 1);
            int end = s.lastIndexOf("```");
            if (end > 0) s = s.substring(0, end).trim();
        }
        // 若清洗后仍非以 { 开头，尝试提取第一个完整 JSON 对象（应对 AI 在 JSON 前输出叙述/旁白）
        if (!s.startsWith("{")) {
            int first = s.indexOf('{');
            int last = s.lastIndexOf('}');
            if (first >= 0 && last > first) {
                log.debug("[小结] 从非 JSON 前缀中提取 {} 区间");
                s = s.substring(first, last + 1);
            }
        }
        return s;
    }

    /**
     * 从已解析的小结 JSON 中写入关键人物到 key_person 表，并绑定到当前小结（用户可后续编辑）。
     * 使用字段：key_characters = [ { "name": "xxx", "role_label": "xxx" }, ... ]
     */
    private void parseAndSaveKeyPersons(JsonNode root, Long summaryId, Long userId, Long sessionId, Long participantId) {
        summaryKeyPersonRepository.deleteBySummaryId(summaryId);
        JsonNode arr = root.path("key_characters");
        if (!arr.isArray()) return;
        for (JsonNode n : arr) {
            String name = n.path("name").asText(null);
            if (name == null || name.isBlank()) continue;
            String roleLabel = n.has("role_label") ? n.path("role_label").asText(null) : null;
            try {
                KeyPerson k = keyPersonService.createForSession(userId, sessionId, participantId, name, roleLabel);
                BoardSummaryKeyPerson link = new BoardSummaryKeyPerson();
                link.setSummaryId(summaryId);
                link.setKeyPersonId(k.getId());
                summaryKeyPersonRepository.save(link);
            } catch (Exception ignored) {
                // 单条失败不阻塞，继续处理其余
            }
        }
    }

    /**
     * 从已解析的小结 JSON 中写入核心要点到 summary_item 表（事实类/表达类），用户可后续编辑。
     * 使用字段：core_points = [ { "type": "事实类|表达类", "content": "xxx" }, ... ]
     */
    private void parseAndSaveItems(JsonNode root, Long summaryId) {
        itemRepository.findBySummaryIdOrderByItemOrderAsc(summaryId).forEach(itemRepository::delete);
        JsonNode core = root.path("core_points");
        if (!core.isArray()) {
            saveFallbackItem(summaryId);
            return;
        }
        int order = 0;
        for (JsonNode n : core) {
            String typeStr = n.path("type").asText(null);
            String content = n.path("content").asText(null);
            if (content == null || content.isBlank()) continue;
            if (content.length() > 500) content = content.substring(0, 500);
            SummaryItem item = new SummaryItem();
            item.setSummaryId(summaryId);
            item.setItemType("表达类".equals(typeStr) ? SummaryItemType.EXPRESSION : SummaryItemType.FACT);
            item.setContent(content);
            item.setItemOrder(order++);
            item.setIsSelected(true);
            itemRepository.save(item);
        }
        if (order == 0) saveFallbackItem(summaryId);
    }

    private void saveFallbackItem(Long summaryId) {
        SummaryItem fallback = new SummaryItem();
        fallback.setSummaryId(summaryId);
        fallback.setItemType(SummaryItemType.FACT);
        fallback.setContent("[解析失败或暂无要点，请手动编辑]");
        fallback.setItemOrder(0);
        fallback.setIsSelected(true);
        itemRepository.save(fallback);
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
     * 将小结勾选条目写入素材快照，并推进参与者的板块进度（生成小结时内部调用，生成即确认）
     */
    private void applyConfirm(BoardSummary summary, Long userId) {
        Long summaryId = summary.getId();
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

        var session = sessionRepository.findById(summary.getSessionId()).orElse(null);
        if (session != null) {
            session.setStatus(SessionStatus.COMPLETED);
            sessionRepository.save(session);
        }
    }

    /**
     * 设置小结绑定的关键人物（需全部属于当前用户）
     */
    @Transactional
    public void setSummaryKeyPersons(Long summaryId, List<Long> keyPersonIds, Long userId) {
        var summary = summaryRepository.findById(summaryId).orElseThrow(() -> new BusinessException(404, "小结不存在"));
        checkSessionAccess(summary.getSessionId(), userId);
        if (summary.getStatus() == SummaryStatus.CONFIRMED) throw new BusinessException(400, "已确认的小结不可修改绑定角色");
        if (keyPersonIds != null) {
            for (Long pid : keyPersonIds) {
                if (!keyPersonService.belongsToUser(pid, userId)) throw new BusinessException(403, "关键人物不属于当前用户");
            }
        }
        summaryKeyPersonRepository.deleteBySummaryId(summaryId);
        if (keyPersonIds != null && !keyPersonIds.isEmpty()) {
            for (Long keyPersonId : keyPersonIds) {
                BoardSummaryKeyPerson link = new BoardSummaryKeyPerson();
                link.setSummaryId(summaryId);
                link.setKeyPersonId(keyPersonId);
                summaryKeyPersonRepository.save(link);
            }
        }
    }

    private List<KeyPersonResponse> loadKeyPersonsForSummary(Long summaryId) {
        var bindings = summaryKeyPersonRepository.findBySummaryIdOrderByIdAsc(summaryId);
        if (bindings.isEmpty()) return List.of();
        var ids = bindings.stream().map(BoardSummaryKeyPerson::getKeyPersonId).collect(Collectors.toList());
        return keyPersonService.listByIds(ids);
    }

    private BoardSummaryResponse toSummaryResponse(BoardSummary s) {
        var items = itemRepository.findBySummaryIdOrderByItemOrderAsc(s.getId()).stream().map(this::toItemResponse).collect(Collectors.toList());
        var keyPersons = loadKeyPersonsForSummary(s.getId());
        String boardCode = null, boardName = null;
        var pbOpt = projectBoardRepository.findById(s.getProjectBoardId());
        if (pbOpt.isPresent()) {
            var bmOpt = boardMetaRepository.findById(pbOpt.get().getBoardMetaId());
            if (bmOpt.isPresent()) { boardCode = bmOpt.get().getCode(); boardName = bmOpt.get().getName(); }
        }
        return BoardSummaryResponse.builder().id(s.getId()).sessionId(s.getSessionId()).projectBoardId(s.getProjectBoardId()).boardCode(boardCode).boardName(boardName).versionNo(s.getVersionNo()).status(s.getStatus().name()).title(s.getTitle()).generatedAt(s.getGeneratedAt()).confirmedAt(s.getConfirmedAt()).items(items).keyPersons(keyPersons).build();
    }

    private SummaryItemResponse toItemResponse(SummaryItem i) {
        return SummaryItemResponse.builder().id(i.getId()).summaryId(i.getSummaryId()).itemType(i.getItemType().name()).content(i.getContent()).itemOrder(i.getItemOrder()).isSelected(i.getIsSelected()).createdAt(i.getCreatedAt()).build();
    }
}
