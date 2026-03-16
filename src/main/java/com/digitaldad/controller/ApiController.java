package com.digitaldad.controller;

import com.digitaldad.dto.SpeechQuotaResponse;
import com.digitaldad.service.SpeechTranscriptionQuotaService;
import com.digitaldad.common.result.Result;
import com.digitaldad.dto.*;
import com.digitaldad.enums.ParticipantRole;
import com.digitaldad.service.*;
import com.digitaldad.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 统一 API 控制器（C 端 + B 端）
 * <p>汇聚 C 端（参与者）和 B 端（主持人）的核心业务接口，包括项目、会话、消息、小结、故事、人物、交付物等。</p>
 */
@RestController
@RequiredArgsConstructor
public class ApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    private final ProjectService projectService;
    private final ProjectParticipantService participantService;
    private final ProjectParticipantStatusService participantStatusService;
    private final InterviewSessionService sessionService;
    private final BoardSummaryService summaryService;
    private final BoardStoryService storyService;
    private final KeyPersonService personService;
    private final SpeechTranscriptionQuotaService speechQuotaService;

    // ========== C 端 - 入口与绑定 ==========

    /**
     * 通过分享令牌获取项目基本信息（无需登录）
     *
     * @param token 项目分享令牌
     * @return 项目信息，包含名称、描述等
     */
    @GetMapping("/api/c/entry/{token}")
    public Result<ProjectInfoResponse> getProjectInfo(@PathVariable String token) {
        return Result.ok(projectService.getProjectInfoByToken(token));
    }

    /**
     * 获取当前用户的语音转写配额
     *
     * @param principal 当前登录用户
     * @return 剩余秒数和已使用秒数
     */
    @GetMapping("/api/c/speech-quota")
    public Result<SpeechQuotaResponse> getSpeechQuota(@AuthenticationPrincipal UserPrincipal principal) {
        var dto = speechQuotaService.getOrInitQuota(principal.getUserId());
        return Result.ok(new SpeechQuotaResponse(dto.remainingSeconds(), dto.totalUsedSeconds()));
    }

    /**
     * 获取当前用户在该项目下的状态（步骤、参与者、会话、板块、小结等）
     * <p>前端根据 step 及 sessionId/currentSummaryId 决定展示选身份、开始采访、对话页、小结确认页或完成页。</p>
     *
     * @param principal 当前登录用户
     * @param projectId 项目 ID
     * @return 状态响应，含 step、sessionId、boards、currentSummaryId 等（C 端不暴露参与者 ID）
     */
    @GetMapping("/api/c/projects/{projectId}/my-status")
    public Result<ProjectMyStatusResponse> getMyStatusInProject(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long projectId) {
        log.info("[C端] GET my-status projectId={} userId={}", projectId, principal.getUserId());
        var data = participantStatusService.getMyStatus(projectId, principal.getUserId());
        log.info("[C端] my-status step={} sessionId={} currentProjectBoardId={}", data.getStep(), data.getSessionId(), data.getCurrentProjectBoardId());
        return Result.ok(data);
    }

    // ========== C 端 - 会话 ==========

    /**
     * 进入项目某板块采访：未绑定时先绑定再创建/恢复该板块会话，已绑定则直接创建或恢复该板块会话。
     * <p>传 projectId + projectBoardId；若用户尚未绑定该项目，request.role 必填（GROOM/BRIDE）。换板块会产生新 session。</p>
     *
     * @param principal 当前登录用户
     * @param request   包含 projectId、projectBoardId（必填），未绑定时必填 role
     * @return 会话信息（含 role）
     */
    @PostMapping("/api/c/sessions")
    public Result<SessionResponse> createOrResume(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody CreateSessionRequest request) {
        Long projectId = request.getProjectId();
        Long projectBoardId = request.getProjectBoardId();
        Long userId = principal.getUserId();
        log.info("[C端] POST sessions projectId={} projectBoardId={} userId={} role={}", projectId, projectBoardId, userId, request.getRole());
        if (participantService.findByProjectAndUser(projectId, userId).isEmpty()) {
            if (request.getRole() == null || request.getRole().isBlank()) {
                throw new com.digitaldad.common.exception.BusinessException(400, "请选择身份（新郎/新娘）");
            }
            ParticipantRole role = ParticipantRole.valueOf(request.getRole().toUpperCase());
            participantService.bindParticipant(projectId, userId, role);
        }
        var data = sessionService.createOrResume(projectId, projectBoardId, userId);
        log.info("[C端] sessions created sessionId={} currentProjectBoardId={}", data.getId(), data.getCurrentProjectBoardId());
        return Result.ok(data);
    }

    /**
     * 获取会话详情
     *
     * @param principal  当前登录用户
     * @param sessionId  会话 ID
     * @return 会话信息
     */
    @GetMapping("/api/c/sessions/{sessionId}")
    public Result<SessionResponse> getSession(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long sessionId) {
        log.info("[C端] GET session sessionId={} userId={}", sessionId, principal.getUserId());
        return Result.ok(sessionService.getSession(sessionId, principal.getUserId()));
    }

    /**
     * 获取会话中的消息列表
     *
     * @param principal  当前登录用户
     * @param sessionId  会话 ID
     * @return 消息列表
     */
    @GetMapping("/api/c/sessions/{sessionId}/messages")
    public Result<MessagesListResponse> getMessages(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long sessionId) {
        log.info("[C端] GET messages sessionId={} userId={}", sessionId, principal.getUserId());
        var data = sessionService.getMessages(sessionId, principal.getUserId());
        log.info("[C端] messages count={} totalRounds={}", data.getMessages() != null ? data.getMessages().size() : 0, data.getTotalRounds());
        return Result.ok(data);
    }

    /**
     * 发送消息（文本或语音转写）
     *
     * @param principal  当前登录用户
     * @param sessionId  会话 ID
     * @param request    消息内容
     * @return 新创建的消息
     */
    @PostMapping("/api/c/sessions/{sessionId}/messages")
    public Result<MessageResponse> sendMessage(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long sessionId, @Valid @RequestBody SendMessageRequest request) {
        log.info("[C端] POST message sessionId={} userId={} contentLen={}", sessionId, principal.getUserId(), request.getContent() != null ? request.getContent().length() : 0);
        var data = sessionService.sendMessage(sessionId, principal.getUserId(), request);
        log.info("[C端] message created messageId={}", data.getId());
        return Result.ok(data);
    }

    /**
     * 更新消息内容
     *
     * @param principal  当前登录用户
     * @param sessionId  会话 ID
     * @param messageId  消息 ID
     * @param request    更新内容
     * @return 更新后的消息
     */
    @PutMapping("/api/c/sessions/{sessionId}/messages/{messageId}")
    public Result<MessageResponse> updateMessage(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long sessionId, @PathVariable Long messageId, @Valid @RequestBody UpdateMessageRequest request) {
        return Result.ok(sessionService.updateMessage(sessionId, messageId, principal.getUserId(), request));
    }

    /**
     * 删除消息
     *
     * @param principal  当前登录用户
     * @param sessionId  会话 ID
     * @param messageId  消息 ID
     * @return 成功时返回空结果
     */
    @DeleteMapping("/api/c/sessions/{sessionId}/messages/{messageId}")
    public Result<Void> deleteMessage(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long sessionId, @PathVariable Long messageId) {
        sessionService.deleteMessage(sessionId, messageId, principal.getUserId());
        return Result.ok();
    }

    /**
     * 提交会话，完成采访
     *
     * @param principal  当前登录用户
     * @param sessionId  会话 ID
     * @return 提交结果
     */
    @PostMapping("/api/c/sessions/{sessionId}/submit")
    public Result<SubmitResultResponse> submit(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long sessionId) {
        log.info("[C端] POST submit sessionId={} userId={}", sessionId, principal.getUserId());
        var data = sessionService.submit(sessionId, principal.getUserId());
        log.info("[C端] submit done roundCount={} newMessages={}", data.getRoundCount(), data.getNewMessages() != null ? data.getNewMessages().size() : 0);
        return Result.ok(data);
    }

    // ========== C 端 - 小结 ==========

    /**
     * 创建并确认对话小结（AI 生成即确认：写入素材快照并推进板块进度）
     *
     * @param principal  当前登录用户
     * @param sessionId  会话 ID
     * @return 小结内容及条目列表（状态为已确认）
     */
    @PostMapping("/api/c/sessions/{sessionId}/summaries")
    public Result<BoardSummaryResponse> createSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sessionId,
            @RequestParam(required = false) Boolean useTestData) {
        // 默认使用测试数据；仅当 useTestData 不为空且为 true 时才请求 AI 使用真实数据
        boolean useRealData = Boolean.TRUE.equals(useTestData);
        log.info("[C端] POST summaries sessionId={} userId={} useTestData={} useRealData={}", sessionId, principal.getUserId(), useTestData, useRealData);
        var data = summaryService.createSummary(sessionId, principal.getUserId(), useRealData);
        log.info("[C端] summary created and confirmed summaryId={} status={}", data.getId(), data.getStatus());
        return Result.ok(data);
    }

    /**
     * 获取当前会话最新小结
     *
     * @param principal  当前登录用户
     * @param sessionId  会话 ID
     * @return 当前小结，若无则返回最新的一条
     */
    @GetMapping("/api/c/sessions/{sessionId}/summaries/current")
    public Result<BoardSummaryResponse> getCurrentSummary(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long sessionId) {
        return Result.ok(summaryService.getCurrentSummary(sessionId, principal.getUserId()));
    }

    /**
     * 根据 ID 获取小结详情
     *
     * @param principal  当前登录用户
     * @param summaryId  小结 ID
     * @return 小结详情
     */
    @GetMapping("/api/c/board-summaries/{summaryId}")
    public Result<BoardSummaryResponse> getSummary(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long summaryId) {
        return Result.ok(summaryService.getSummary(summaryId, principal.getUserId()));
    }

    /**
     * 更新小结条目
     *
     * @param principal 当前登录用户
     * @param itemId   条目 ID
     * @param request  更新内容
     * @return 更新后的条目
     */
    @PutMapping("/api/c/summary-items/{itemId}")
    public Result<SummaryItemResponse> updateItem(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long itemId, @Valid @RequestBody UpdateSummaryItemRequest request) {
        return Result.ok(summaryService.updateItem(itemId, principal.getUserId(), request));
    }

    /**
     * 在小结中添加条目
     *
     * @param principal  当前登录用户
     * @param summaryId  小结 ID
     * @param request    条目内容
     * @return 新增的条目
     */
    @PostMapping("/api/c/board-summaries/{summaryId}/items")
    public Result<SummaryItemResponse> addItem(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long summaryId, @Valid @RequestBody AddSummaryItemRequest request) {
        return Result.ok(summaryService.addItem(summaryId, principal.getUserId(), request));
    }

    /**
     * 删除小结条目
     *
     * @param principal 当前登录用户
     * @param itemId   条目 ID
     * @return 成功时返回空结果
     */
    @DeleteMapping("/api/c/summary-items/{itemId}")
    public Result<Void> deleteItem(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long itemId) {
        summaryService.deleteItem(itemId, principal.getUserId());
        return Result.ok();
    }

    /**
     * 设置小结绑定的关键人物（角色需属于当前用户）
     *
     * @param principal  当前登录用户
     * @param summaryId  小结 ID
     * @param request    keyPersonIds 列表
     * @return 成功时返回空结果
     */
    @PutMapping("/api/c/board-summaries/{summaryId}/key-persons")
    public Result<Void> setSummaryKeyPersons(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long summaryId, @Valid @RequestBody SetSummaryKeyPersonsRequest request) {
        summaryService.setSummaryKeyPersons(summaryId, request.getKeyPersonIds(), principal.getUserId());
        return Result.ok();
    }

    // ========== C 端 - 故事 ==========

    /**
     * 当前用户参与的所有项目下的板块列表（仅需登录 token，用于按板块查询故事等）
     * <p>返回该用户作为参与者的每个项目中的全部板块；用返回的 projectBoardId 调用 GET /api/c/users/me/stories?projectBoardId=xxx 查询该板块的故事。</p>
     *
     * @param principal 当前登录用户
     * @return 板块列表（projectId、projectBoardId、boardCode、boardName、displayOrder），按 projectId、displayOrder 排序
     */
    @GetMapping("/api/c/users/me/all-boards")
    public Result<List<UserBoardItemDto>> listAllMyBoards(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(participantService.listAllBoardsForUser(principal.getUserId()));
    }

    /**
     * 当前用户有故事的项目板块列表（用于故事页按板块 Tab/筛选）
     * <p>仅返回用户有故事记录的板块；按某板块查故事时用返回的 projectBoardId 传 GET /api/c/users/me/stories?projectBoardId=xxx。</p>
     *
     * @param principal 当前登录用户
     * @return 板块列表（projectId、projectBoardId、boardCode、boardName、displayOrder）
     */
    @GetMapping("/api/c/users/me/boards")
    public Result<List<UserBoardItemDto>> listMyBoards(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(storyService.listBoardsWithStories(principal.getUserId()));
    }

    /**
     * 当前用户查询自己的全部故事，支持按会话、板块、角色筛选，分页返回。
     *
     * @param principal      当前登录用户
     * @param sessionId      可选，限定某次会话
     * @param projectBoardId 可选，限定某项目板块（按板块筛选用此参数）
     * @param boardCode      可选，限定板块类型编码
     * @param roleLabel      可选，限定包含该角色的会话下的故事（关键人物角色标签）
     * @param page           页码，从 1 开始，默认 1
     * @param size           每页条数，默认 10，最大 50
     * @return 分页结果（content 为当前页故事列表）
     */
    @GetMapping("/api/c/users/me/stories")
    public Result<Page<BoardStoryResponse>> listMyStories(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long projectBoardId,
            @RequestParam(required = false) String boardCode,
            @RequestParam(required = false) String roleLabel,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(storyService.listMyStories(principal.getUserId(), sessionId, projectBoardId, boardCode, roleLabel, page, size));
    }

    /**
     * 创建故事（AI 根据当前会话当前板块的对话生成）
     *
     * @param principal  当前登录用户
     * @param sessionId  会话 ID
     * @return 故事内容
     */
    @PostMapping("/api/c/sessions/{sessionId}/stories")
    public Result<BoardStoryResponse> createStory(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long sessionId) {
        log.info("[C端] POST stories sessionId={} userId={}", sessionId, principal.getUserId());
        var data = storyService.createStory(sessionId, principal.getUserId());
        log.info("[C端] story created storyId={} projectBoardId={}", data != null ? data.getId() : null, data != null ? data.getProjectBoardId() : null);
        return Result.ok(data);
    }

    /**
     * 按会话 + 板块获取单条故事（用于详情或会话内某板块故事）
     *
     * @param principal      当前登录用户
     * @param sessionId      会话 ID
     * @param projectBoardId 项目板块 ID
     * @return 故事内容，无则 data 为 null
     */
    @GetMapping("/api/c/sessions/{sessionId}/stories")
    public Result<BoardStoryResponse> getStory(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long sessionId, @RequestParam Long projectBoardId) {
        log.info("[C端] GET story sessionId={} projectBoardId={} userId={}", sessionId, projectBoardId, principal.getUserId());
        var data = storyService.getStory(sessionId, projectBoardId, principal.getUserId());
        log.info("[C端] story get storyId={}", data != null ? data.getId() : null);
        return Result.ok(data);
    }

    // ========== C 端 - 人物 ==========

    /**
     * 列出当前用户的角色库（用户维度，与项目/会话无关）
     *
     * @param principal 当前登录用户
     * @return 关键人物列表
     */
    @GetMapping("/api/c/users/me/key-persons")
    public Result<List<KeyPersonResponse>> listMyKeyPersons(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(personService.listByUser(principal.getUserId()));
    }

    /**
     * 当前用户新增关键人物（写入角色库，用于「我的-关键人物」等入口）
     *
     * @param principal 当前登录用户
     * @param request   人物信息
     * @return 新增的人物
     */
    @PostMapping("/api/c/users/me/key-persons")
    public Result<KeyPersonResponse> addMyKeyPerson(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody AddKeyPersonRequest request) {
        return Result.ok(personService.addByUser(principal.getUserId(), request));
    }

    /**
     * 列出会话中的关键人物（兼容：返回该用户整份角色库）
     *
     * @param principal  当前登录用户
     * @param sessionId  会话 ID
     * @return 人物列表
     */
    @GetMapping("/api/c/sessions/{sessionId}/persons")
    public Result<List<KeyPersonResponse>> listPersons(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long sessionId) {
        return Result.ok(personService.list(sessionId, principal.getUserId()));
    }

    /**
     * 在会话下添加关键人物（写入用户角色库，并记录创建时会话）
     *
     * @param principal  当前登录用户
     * @param sessionId  会话 ID
     * @param request    人物信息
     * @return 新增的人物
     */
    @PostMapping("/api/c/sessions/{sessionId}/persons")
    public Result<KeyPersonResponse> addPerson(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long sessionId, @Valid @RequestBody AddKeyPersonRequest request) {
        return Result.ok(personService.add(sessionId, principal.getUserId(), request));
    }

    /**
     * 更新关键人物信息
     *
     * @param principal 当前登录用户
     * @param personId  人物 ID
     * @param request   更新内容
     * @return 更新后的人物
     */
    @PutMapping("/api/c/key-persons/{personId}")
    public Result<KeyPersonResponse> updatePerson(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long personId, @Valid @RequestBody UpdateKeyPersonRequest request) {
        return Result.ok(personService.update(personId, principal.getUserId(), request));
    }

    /**
     * 删除关键人物
     *
     * @param principal 当前登录用户
     * @param personId  人物 ID
     * @return 成功时返回空结果
     */
    @DeleteMapping("/api/c/key-persons/{personId}")
    public Result<Void> deletePerson(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long personId) {
        personService.delete(personId, principal.getUserId());
        return Result.ok();
    }

}
