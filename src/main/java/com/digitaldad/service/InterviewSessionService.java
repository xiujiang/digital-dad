package com.digitaldad.service;

import com.digitaldad.entity.BoardMeta;
import com.digitaldad.entity.ProjectBoard;
import com.digitaldad.repository.BoardMetaRepository;
import com.digitaldad.repository.ProjectBoardRepository;
import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.service.ConfigService;
import com.digitaldad.dto.ChatMessage;
import com.digitaldad.dto.*;
import com.digitaldad.entity.ConversationMessage;
import com.digitaldad.entity.InterviewSession;
import com.digitaldad.entity.ProjectParticipant;
import com.digitaldad.entity.SessionBoardRounds;
import com.digitaldad.enums.MessageType;
import com.digitaldad.enums.SenderType;
import com.digitaldad.enums.SessionStatus;
import com.digitaldad.repository.ConversationMessageRepository;
import com.digitaldad.repository.InterviewSessionRepository;
import com.digitaldad.repository.ProjectParticipantRepository;
import com.digitaldad.repository.SessionBoardRoundsRepository;
import com.digitaldad.dto.PromptContentDto;
import com.digitaldad.enums.PromptRoleType;
import com.digitaldad.service.PromptSupplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 采访会话服务
 * <p>管理采访会话的创建、恢复、消息收发、提交及 AI 回复生成。</p>
 */
@Service
@RequiredArgsConstructor
public class InterviewSessionService {

    private final InterviewSessionRepository sessionRepository;
    private final ConversationMessageRepository messageRepository;
    private final ProjectParticipantRepository participantRepository;
    private final ProjectBoardRepository projectBoardRepository;
    private final BoardMetaRepository boardMetaRepository;
    private final SessionBoardRoundsRepository sessionBoardRoundsRepository;
    private final PromptSupplyService promptSupplyService;
    private final AiChatService aiChatService;
    private final ConfigService configService;

    private InterviewSession checkSessionAccess(Long sessionId, Long userId) {
        InterviewSession s = sessionRepository.findById(sessionId).orElseThrow(() -> new BusinessException(404, "会话不存在"));
        ProjectParticipant p = participantRepository.findById(s.getParticipantId()).orElseThrow(() -> new BusinessException(404, "参与者不存在"));
        if (!p.getUserId().equals(userId)) throw new BusinessException(403, "无权限操作该会话");
        return s;
    }

    /**
     * 创建或恢复「该参与者在该板块下」的会话；若已有进行中的则直接返回。
     * <p>session = 用户 + 板块，换板块会产生新 session。</p>
     *
     * @param projectId     项目 ID
     * @param projectBoardId 板块 ID（必填）
     * @param userId         当前用户 ID
     * @return 会话信息
     */
    @Transactional
    public SessionResponse createOrResume(Long projectId, Long projectBoardId, Long userId) {
        ProjectParticipant participant = participantRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(404, "未绑定该项目，请先选择身份"));
        Long participantId = participant.getId();
        if (!participant.getProjectId().equals(projectId)) {
            throw new BusinessException(400, "板块不属于当前项目");
        }
        ProjectBoard board = projectBoardRepository.findById(projectBoardId)
                .orElseThrow(() -> new BusinessException(404, "板块不存在"));
        if (!board.getProjectId().equals(projectId)) {
            throw new BusinessException(400, "板块不属于当前项目");
        }

        // 唯一约束 uk_participant_board：同一参与者同一板块仅一条会话。先按 (participant, board) 查，有则直接返回，避免 COMPLETED 等状态被漏查导致重复插入
        var existingAny = sessionRepository.findByParticipantIdAndCurrentProjectBoardId(participantId, projectBoardId);
        if (existingAny.isPresent()) {
            InterviewSession s = existingAny.get();
            s.setLastActiveAt(LocalDateTime.now());
            sessionRepository.save(s);
            return toSessionResponse(s, participant);
        }

        InterviewSession session = new InterviewSession();
        session.setProjectId(participant.getProjectId());
        session.setParticipantId(participantId);
        session.setCurrentProjectBoardId(projectBoardId);
        session.setStatus(SessionStatus.ACTIVE);
        session.setRoundCount(0);
        session.setStartedAt(LocalDateTime.now());
        session.setLastActiveAt(LocalDateTime.now());
        session = sessionRepository.save(session);

        SessionBoardRounds sbr = new SessionBoardRounds();
        sbr.setSessionId(session.getId());
        sbr.setProjectBoardId(projectBoardId);
        sbr.setRoundCount(0);
        sessionBoardRoundsRepository.save(sbr);

        String welcomeContent = buildWelcomeMessage(board);
        ConversationMessage welcomeMsg = new ConversationMessage();
        welcomeMsg.setSessionId(session.getId());
        welcomeMsg.setSenderType(SenderType.AI);
        welcomeMsg.setMessageType(MessageType.TEXT);
        welcomeMsg.setContent(welcomeContent);
        welcomeMsg.setSequenceNo(1);
        welcomeMsg.setBatchNo(0);
        welcomeMsg.setIsSubmitted(true);
        messageRepository.save(welcomeMsg);

        return toSessionResponse(session, participant);
    }

    /**
     * 构建当前板块的欢迎语（从提示词表 code=BOARD_INTERVIEW_WELCOME 读取模板，替换 {{boardName}}）
     */
    private String buildWelcomeMessage(ProjectBoard firstBoard) {
        BoardMeta meta = firstBoard.getBoardMetaId() != null
                ? boardMetaRepository.findById(firstBoard.getBoardMetaId()).orElse(null)
                : null;
        String boardName = meta != null ? meta.getName() : "当前板块";
        try {
            String template = promptSupplyService.getActiveContentByCode("BOARD_INTERVIEW_WELCOME");
            if (template != null && !template.isBlank()) {
                return template.replace("{{boardName}}", boardName);
            }
        } catch (Exception ignored) {
            // 提示词未配置或未生效时使用兜底文案
        }
        return String.format("我是%s板块，我们来一起聊聊你的%s方面的事情，我们可以开始了。", boardName, boardName);
    }

    /**
     * 获取会话详情
     *
     * @param sessionId 会话 ID
     * @param userId    当前用户 ID
     * @return 会话信息
     */
    public SessionResponse getSession(Long sessionId, Long userId) {
        InterviewSession session = checkSessionAccess(sessionId, userId);
        ProjectParticipant participant = participantRepository.findById(session.getParticipantId()).orElseThrow(() -> new BusinessException(404, "参与者不存在"));
        return toSessionResponse(session, participant);
    }

    /**
     * 获取会话中的消息列表（按序号排序）及总轮数
     *
     * @param sessionId 会话 ID
     * @param userId    当前用户 ID
     * @return 消息列表与总轮数
     */
    public MessagesListResponse getMessages(Long sessionId, Long userId) {
        InterviewSession session = checkSessionAccess(sessionId, userId);
        List<MessageResponse> messages = messageRepository.findBySessionIdOrderBySequenceNoAsc(sessionId).stream().map(this::toMessageResponse).collect(Collectors.toList());
        return MessagesListResponse.builder()
                .messages(messages)
                .totalRounds(session.getRoundCount() != null ? session.getRoundCount() : 0)
                .build();
    }

    /**
     * 发送消息（文本或语音转写）
     *
     * @param sessionId 会话 ID
     * @param userId    当前用户 ID
     * @param request   消息内容
     * @return 新创建的消息
     */
    @Transactional
    public MessageResponse sendMessage(Long sessionId, Long userId, SendMessageRequest request) {
        InterviewSession session = checkSessionAccess(sessionId, userId);
        int maxSeq = messageRepository.findBySessionIdOrderBySequenceNoAsc(sessionId).stream().mapToInt(ConversationMessage::getSequenceNo).max().orElse(0);

        ConversationMessage msg = new ConversationMessage();
        msg.setSessionId(sessionId);
        msg.setSenderType(SenderType.USER);
        msg.setMessageType(request.getAudioUrl() != null ? MessageType.AUDIO : MessageType.TEXT);
        msg.setContent(request.getContent() != null ? request.getContent() : request.getTranscriptText());
        msg.setAudioUrl(request.getAudioUrl());
        msg.setTranscriptText(request.getTranscriptText());
        msg.setSequenceNo(maxSeq + 1);
        msg.setBatchNo(0);
        msg.setIsSubmitted(false);
        msg = messageRepository.save(msg);

        session.setLastActiveAt(LocalDateTime.now());
        sessionRepository.save(session);
        return toMessageResponse(msg);
    }

    /**
     * 删除消息（已提交的消息不可删除）
     *
     * @param sessionId 会话 ID
     * @param messageId 消息 ID
     * @param userId    当前用户 ID
     */
    @Transactional
    public void deleteMessage(Long sessionId, Long messageId, Long userId) {
        checkSessionAccess(sessionId, userId);
        ConversationMessage msg = messageRepository.findBySessionIdAndId(sessionId, messageId).orElseThrow(() -> new BusinessException(404, "消息不存在"));
        if (msg.getIsSubmitted()) throw new BusinessException(400, "已提交的消息不可删除");
        messageRepository.delete(msg);
    }

    /**
     * 更新消息内容（已提交的不可修改）
     *
     * @param sessionId 会话 ID
     * @param messageId 消息 ID
     * @param userId    当前用户 ID
     * @param request   更新内容
     * @return 更新后的消息
     */
    @Transactional
    public MessageResponse updateMessage(Long sessionId, Long messageId, Long userId, UpdateMessageRequest request) {
        checkSessionAccess(sessionId, userId);
        ConversationMessage msg = messageRepository.findBySessionIdAndId(sessionId, messageId).orElseThrow(() -> new BusinessException(404, "消息不存在"));
        if (msg.getIsSubmitted()) throw new BusinessException(400, "已提交的消息不可修改");
        if (request.getContent() != null) msg.setContent(request.getContent());
        msg = messageRepository.save(msg);
        return toMessageResponse(msg);
    }

    /**
     * 提交待提交的消息，触发 AI 生成回复并返回新消息
     *
     * @param sessionId 会话 ID
     * @param userId    当前用户 ID
     * @return 提交结果（含新 batchNo、roundCount、AI 消息）
     */
    @Transactional
    public SubmitResultResponse submit(Long sessionId, Long userId) {
        InterviewSession session = checkSessionAccess(sessionId, userId);
        List<ConversationMessage> unsubmitted = messageRepository.findBySessionIdAndIsSubmittedFalse(sessionId);
        if (unsubmitted.isEmpty()) throw new BusinessException(400, "暂无待提交的消息");

        Long boardId = session.getCurrentProjectBoardId();
        if (boardId == null) throw new BusinessException(400, "当前未选定板块，无法提交");

        int maxRounds = configService.getInterviewMaxRoundsPerBoard();
        SessionBoardRounds sbr = sessionBoardRoundsRepository.findBySessionIdAndProjectBoardId(sessionId, boardId)
                .orElseGet(() -> {
                    SessionBoardRounds newSbr = new SessionBoardRounds();
                    newSbr.setSessionId(sessionId);
                    newSbr.setProjectBoardId(boardId);
                    newSbr.setRoundCount(0);
                    return sessionBoardRoundsRepository.save(newSbr);
                });
        if (sbr.getRoundCount() >= maxRounds) {
            throw new BusinessException(400, "该板块聊天轮数已达上限（" + maxRounds + " 轮），请前往下一板块");
        }

        int nextBatch = messageRepository.findBySessionIdOrderBySequenceNoAsc(sessionId).stream().mapToInt(ConversationMessage::getBatchNo).max().orElse(0) + 1;
        for (ConversationMessage m : unsubmitted) {
            m.setBatchNo(nextBatch);
            m.setIsSubmitted(true);
            messageRepository.save(m);
        }

        List<String> userContents = unsubmitted.stream().map(ConversationMessage::getContent).filter(c -> c != null && !c.isBlank()).collect(Collectors.toList());
        if (userContents.isEmpty()) userContents = List.of("[用户发送了语音或空消息]");

        ProjectParticipant participant = participantRepository.findById(session.getParticipantId()).orElseThrow();
        ProjectBoard projectBoard = session.getCurrentProjectBoardId() != null ? projectBoardRepository.findById(session.getCurrentProjectBoardId()).orElse(null) : null;
        String boardCode = projectBoard != null ? boardMetaRepository.findById(projectBoard.getBoardMetaId()).map(BoardMeta::getCode).orElse("COMMON") : "COMMON";
        PromptRoleType role = participant.getRoleType() != null ? PromptRoleType.valueOf(participant.getRoleType().name()) : PromptRoleType.COMMON;

        String systemPrompt = "";
        try {
            List<PromptContentDto> prompts = promptSupplyService.getInterviewPrompts(boardCode, role);
            systemPrompt = prompts.stream().map(PromptContentDto::getContent).collect(Collectors.joining("\n\n"));
        } catch (Exception e) {
            systemPrompt = "你是数字爸爸的采访助手，温和、共情，引导用户分享故事。";
        }

        // 构建带历史上下文的对话消息
        List<ChatMessage> history = messageRepository.findBySessionIdOrderBySequenceNoAsc(sessionId).stream()
                .filter(m -> m.getContent() != null && !m.getContent().isBlank())
                .map(m -> "USER".equals(m.getSenderType().name()) ? ChatMessage.user(m.getContent()) : ChatMessage.assistant(m.getContent()))
                .collect(Collectors.toList());
        String aiReply = aiChatService.chatWithHistory(systemPrompt, history);

        int maxSeq = messageRepository.findBySessionIdOrderBySequenceNoAsc(sessionId).stream().mapToInt(ConversationMessage::getSequenceNo).max().orElse(0);
        ConversationMessage aiMsg = new ConversationMessage();
        aiMsg.setSessionId(sessionId);
        aiMsg.setSenderType(SenderType.AI);
        aiMsg.setMessageType(MessageType.TEXT);
        aiMsg.setContent(aiReply);
        aiMsg.setSequenceNo(maxSeq + 1);
        aiMsg.setBatchNo(nextBatch);
        aiMsg.setIsSubmitted(true);
        aiMsg = messageRepository.save(aiMsg);

        session.setRoundCount(session.getRoundCount() + 1);
        session.setLastActiveAt(LocalDateTime.now());
        sessionRepository.save(session);

        sbr.setRoundCount(sbr.getRoundCount() + 1);
        sessionBoardRoundsRepository.save(sbr);

        return SubmitResultResponse.builder().newBatchNo(nextBatch).roundCount(session.getRoundCount()).maxRoundsPerBoard(maxRounds).newMessages(List.of(toMessageResponse(aiMsg))).build();
    }

    /**
     * 流式提交：先校验并标记消息已提交，返回用于调用 AI 流式的上下文；流式结束后调用 saveAiReplyAfterStream 落库。
     *
     * @param sessionId 会话 ID
     * @param userId    当前用户 ID
     * @return 含 systemPrompt、history、nextBatch，供 AI 流式调用
     */
    @Transactional
    public PrepareSubmitStreamResult prepareSubmitStream(Long sessionId, Long userId) {
        InterviewSession session = checkSessionAccess(sessionId, userId);
        List<ConversationMessage> unsubmitted = messageRepository.findBySessionIdAndIsSubmittedFalse(sessionId);
        if (unsubmitted.isEmpty()) throw new BusinessException(400, "暂无待提交的消息");

        Long boardId = session.getCurrentProjectBoardId();
        if (boardId == null) throw new BusinessException(400, "当前未选定板块，无法提交");

        int maxRounds = configService.getInterviewMaxRoundsPerBoard();
        SessionBoardRounds sbr = sessionBoardRoundsRepository.findBySessionIdAndProjectBoardId(sessionId, boardId)
                .orElseGet(() -> {
                    SessionBoardRounds newSbr = new SessionBoardRounds();
                    newSbr.setSessionId(sessionId);
                    newSbr.setProjectBoardId(boardId);
                    newSbr.setRoundCount(0);
                    return sessionBoardRoundsRepository.save(newSbr);
                });
        if (sbr.getRoundCount() >= maxRounds) {
            throw new BusinessException(400, "该板块聊天轮数已达上限（" + maxRounds + " 轮），请前往下一板块");
        }

        int nextBatch = messageRepository.findBySessionIdOrderBySequenceNoAsc(sessionId).stream().mapToInt(ConversationMessage::getBatchNo).max().orElse(0) + 1;
        for (ConversationMessage m : unsubmitted) {
            m.setBatchNo(nextBatch);
            m.setIsSubmitted(true);
            messageRepository.save(m);
        }

        ProjectParticipant participant = participantRepository.findById(session.getParticipantId()).orElseThrow();
        ProjectBoard projectBoard = session.getCurrentProjectBoardId() != null ? projectBoardRepository.findById(session.getCurrentProjectBoardId()).orElse(null) : null;
        String boardCode = projectBoard != null ? boardMetaRepository.findById(projectBoard.getBoardMetaId()).map(BoardMeta::getCode).orElse("COMMON") : "COMMON";
        PromptRoleType role = participant.getRoleType() != null ? PromptRoleType.valueOf(participant.getRoleType().name()) : PromptRoleType.COMMON;

        String systemPrompt = "";
        try {
            List<PromptContentDto> prompts = promptSupplyService.getInterviewPrompts(boardCode, role);
            systemPrompt = prompts.stream().map(PromptContentDto::getContent).collect(Collectors.joining("\n\n"));
        } catch (Exception e) {
            systemPrompt = "你是数字爸爸的采访助手，温和、共情，引导用户分享故事。";
        }

        List<ChatMessage> history = messageRepository.findBySessionIdOrderBySequenceNoAsc(sessionId).stream()
                .filter(m -> m.getContent() != null && !m.getContent().isBlank())
                .map(m -> "USER".equals(m.getSenderType().name()) ? ChatMessage.user(m.getContent()) : ChatMessage.assistant(m.getContent()))
                .collect(Collectors.toList());

        return PrepareSubmitStreamResult.builder()
                .systemPrompt(systemPrompt)
                .history(history)
                .nextBatch(nextBatch)
                .build();
    }

    /**
     * 流式提交结束后，将 AI 完整回复落库并更新轮数
     *
     * @param sessionId  会话 ID
     * @param userId     当前用户 ID
     * @param fullContent AI 完整回复内容
     * @param nextBatch  本批 batchNo（与 prepareSubmitStream 返回一致）
     * @return 落库后的消息 ID 与当前轮数
     */
    @Transactional
    public SaveAiReplyResult saveAiReplyAfterStream(Long sessionId, Long userId, String fullContent, int nextBatch) {
        InterviewSession session = checkSessionAccess(sessionId, userId);
        Long boardId = session.getCurrentProjectBoardId();
        if (boardId == null) throw new BusinessException(400, "当前未选定板块");

        int maxSeq = messageRepository.findBySessionIdOrderBySequenceNoAsc(sessionId).stream().mapToInt(ConversationMessage::getSequenceNo).max().orElse(0);
        ConversationMessage aiMsg = new ConversationMessage();
        aiMsg.setSessionId(sessionId);
        aiMsg.setSenderType(SenderType.AI);
        aiMsg.setMessageType(MessageType.TEXT);
        aiMsg.setContent(fullContent != null ? fullContent : "");
        aiMsg.setSequenceNo(maxSeq + 1);
        aiMsg.setBatchNo(nextBatch);
        aiMsg.setIsSubmitted(true);
        aiMsg = messageRepository.save(aiMsg);

        session.setRoundCount(session.getRoundCount() + 1);
        session.setLastActiveAt(LocalDateTime.now());
        sessionRepository.save(session);

        SessionBoardRounds sbr = sessionBoardRoundsRepository.findBySessionIdAndProjectBoardId(sessionId, boardId).orElseThrow();
        sbr.setRoundCount(sbr.getRoundCount() + 1);
        sessionBoardRoundsRepository.save(sbr);

        return SaveAiReplyResult.builder().messageId(aiMsg.getId()).roundCount(session.getRoundCount()).build();
    }

    private SessionResponse toSessionResponse(InterviewSession s, ProjectParticipant p) {
        List<ProjectBoard> boards = projectBoardRepository.findByProjectIdOrderByDisplayOrderAsc(s.getProjectId());
        Map<Long, BoardMeta> metaMap = new HashMap<>();
        boardMetaRepository.findAllById(boards.stream().map(ProjectBoard::getBoardMetaId).distinct().collect(Collectors.toList())).forEach(m -> metaMap.put(m.getId(), m));
        String boardCode = null, boardName = null;
        Integer currentOrder = null;
        if (s.getCurrentProjectBoardId() != null) {
            var pb = boards.stream().filter(b -> b.getId().equals(s.getCurrentProjectBoardId())).findFirst();
            if (pb.isPresent()) {
                BoardMeta bm = metaMap.get(pb.get().getBoardMetaId());
                boardCode = bm != null ? bm.getCode() : null;
                boardName = bm != null ? bm.getName() : null;
                currentOrder = pb.get().getDisplayOrder();
            }
        }
        Long currentBoardId = s.getCurrentProjectBoardId();
        List<BoardInfoDto> boardInfos = boards.stream().map(pb -> {
            BoardMeta bm = metaMap.get(pb.getBoardMetaId());
            boolean completed = s.getStatus() == SessionStatus.COMPLETED || (p.getCurrentBoardOrder() != null && pb.getDisplayOrder() < p.getCurrentBoardOrder());
            return BoardInfoDto.builder().projectBoardId(pb.getId()).boardCode(bm != null ? bm.getCode() : null).boardName(bm != null ? bm.getName() : null).displayOrder(pb.getDisplayOrder()).isCurrent(pb.getId().equals(currentBoardId)).isCompleted(completed).build();
        }).collect(Collectors.toList());
        int currentBoardRounds = 0;
        int maxRounds = configService.getInterviewMaxRoundsPerBoard();
        if (s.getCurrentProjectBoardId() != null) {
            currentBoardRounds = sessionBoardRoundsRepository.findBySessionIdAndProjectBoardId(s.getId(), s.getCurrentProjectBoardId())
                    .map(SessionBoardRounds::getRoundCount).orElse(0);
        }
        return SessionResponse.builder().id(s.getId()).projectId(s.getProjectId()).currentProjectBoardId(s.getCurrentProjectBoardId()).boardCode(boardCode).boardName(boardName).status(s.getStatus().name()).roundCount(s.getRoundCount()).currentBoardRoundCount(currentBoardRounds).maxRoundsPerBoard(maxRounds).startedAt(s.getStartedAt()).lastActiveAt(s.getLastActiveAt()).createdAt(s.getCreatedAt()).currentBoardOrder(currentOrder).boards(boardInfos).role(p.getRoleType() != null ? p.getRoleType().name() : null).build();
    }

    private MessageResponse toMessageResponse(ConversationMessage m) {
        return MessageResponse.builder().id(m.getId()).sessionId(m.getSessionId()).senderType(m.getSenderType().name()).messageType(m.getMessageType().name()).content(m.getContent()).audioUrl(m.getAudioUrl()).sequenceNo(m.getSequenceNo()).batchNo(m.getBatchNo()).isSubmitted(m.getIsSubmitted()).createdAt(m.getCreatedAt()).build();
    }
}
