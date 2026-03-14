package com.digitaldad.project.controller;

import com.digitaldad.ai.dto.SpeechQuotaResponse;
import com.digitaldad.ai.service.SpeechTranscriptionQuotaService;
import com.digitaldad.common.result.Result;
import com.digitaldad.project.dto.*;
import com.digitaldad.project.enums.ParticipantRole;
import com.digitaldad.project.service.*;
import com.digitaldad.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    private final ProjectService projectService;
    private final ProjectParticipantService participantService;
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
     * 绑定参与者和项目，建立参与关系
     *
     * @param principal  当前登录用户
     * @param projectId  项目 ID
     * @param request    绑定请求，包含角色（如新郎/新娘）
     * @return 绑定结果，含 participantId、projectId、role
     */
    @PostMapping("/api/c/projects/{projectId}/bind")
    public Result<BindParticipantResponse> bindParticipant(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long projectId,
            @Valid @RequestBody BindParticipantRequest request) {
        ParticipantRole role = ParticipantRole.valueOf(request.getRole().toUpperCase());
        Long participantId = participantService.bindParticipant(projectId, principal.getUserId(), role);
        return Result.ok(BindParticipantResponse.builder().participantId(participantId).projectId(projectId).role(role.name()).build());
    }

    // ========== C 端 - 会话 ==========

    /**
     * 创建或恢复采访会话
     *
     * @param principal 当前登录用户
     * @param request   包含 participantId
     * @return 会话信息
     */
    @PostMapping("/api/c/sessions")
    public Result<SessionResponse> createOrResume(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody CreateSessionRequest request) {
        return Result.ok(sessionService.createOrResume(request.getParticipantId(), principal.getUserId()));
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
    public Result<List<MessageResponse>> getMessages(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long sessionId) {
        return Result.ok(sessionService.getMessages(sessionId, principal.getUserId()));
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
        return Result.ok(sessionService.sendMessage(sessionId, principal.getUserId(), request));
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
        return Result.ok(sessionService.submit(sessionId, principal.getUserId()));
    }

    // ========== C 端 - 小结 ==========

    /**
     * 创建对话小结（AI 生成）
     *
     * @param principal  当前登录用户
     * @param sessionId  会话 ID
     * @return 小结内容及条目列表
     */
    @PostMapping("/api/c/sessions/{sessionId}/summaries")
    public Result<BoardSummaryResponse> createSummary(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long sessionId) {
        return Result.ok(summaryService.createSummary(sessionId, principal.getUserId()));
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
     * 确认小结，完成编辑
     *
     * @param principal  当前登录用户
     * @param summaryId  小结 ID
     * @return 成功时返回空结果
     */
    @PostMapping("/api/c/board-summaries/{summaryId}/confirm")
    public Result<Void> confirmSummary(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long summaryId) {
        summaryService.confirmSummary(summaryId, principal.getUserId());
        return Result.ok();
    }

    // ========== C 端 - 故事 ==========

    /**
     * 创建故事（AI 根据对话生成）
     *
     * @param principal  当前登录用户
     * @param sessionId  会话 ID
     * @return 故事内容
     */
    @PostMapping("/api/c/sessions/{sessionId}/stories")
    public Result<BoardStoryResponse> createStory(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long sessionId) {
        return Result.ok(storyService.createStory(sessionId, principal.getUserId()));
    }

    /**
     * 获取指定板块的故事
     *
     * @param principal      当前登录用户
     * @param sessionId      会话 ID
     * @param projectBoardId 项目板块 ID
     * @return 故事内容
     */
    @GetMapping("/api/c/sessions/{sessionId}/stories")
    public Result<BoardStoryResponse> getStory(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long sessionId, @RequestParam Long projectBoardId) {
        return Result.ok(storyService.getStory(sessionId, projectBoardId, principal.getUserId()));
    }

    // ========== C 端 - 人物 ==========

    /**
     * 列出会话中的关键人物
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
     * 添加关键人物
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
