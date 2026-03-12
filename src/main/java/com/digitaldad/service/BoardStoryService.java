package com.digitaldad.project.service;

import com.digitaldad.board.entity.BoardMeta;
import com.digitaldad.board.entity.ProjectBoard;
import com.digitaldad.board.repository.BoardMetaRepository;
import com.digitaldad.board.repository.ProjectBoardRepository;
import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.project.dto.BoardStoryResponse;
import com.digitaldad.project.entity.BoardStory;
import com.digitaldad.project.repository.BoardStoryRepository;
import com.digitaldad.project.repository.ConversationMessageRepository;
import com.digitaldad.project.repository.InterviewSessionRepository;
import com.digitaldad.project.repository.ProjectParticipantRepository;
import com.digitaldad.prompt.dto.PromptContentDto;
import com.digitaldad.prompt.service.PromptSupplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * 故事/时光服务
 * <p>根据对话内容 AI 生成板块故事，用于展示温暖的故事叙述。</p>
 */
@Service
@RequiredArgsConstructor
public class BoardStoryService {

    private final BoardStoryRepository storyRepository;
    private final InterviewSessionRepository sessionRepository;
    private final ConversationMessageRepository messageRepository;
    private final ProjectBoardRepository projectBoardRepository;
    private final BoardMetaRepository boardMetaRepository;
    private final PromptSupplyService promptSupplyService;
    private final AiChatService aiChatService;
    private final ProjectParticipantRepository participantRepository;

    private void checkSessionAccess(Long sessionId, Long userId) {
        var session = sessionRepository.findById(sessionId).orElseThrow(() -> new BusinessException(404, "会话不存在"));
        var p = participantRepository.findById(session.getParticipantId()).orElseThrow();
        if (!p.getUserId().equals(userId)) throw new BusinessException(403, "无权限");
    }

    /**
     * 创建/生成板块故事
     */
    @Transactional
    public BoardStoryResponse createStory(Long sessionId, Long userId) {
        checkSessionAccess(sessionId, userId);
        var session = sessionRepository.findById(sessionId).orElseThrow(() -> new BusinessException(404, "会话不存在"));
        var projectBoard = session.getCurrentProjectBoardId() != null ? projectBoardRepository.findById(session.getCurrentProjectBoardId()).orElse(null) : null;
        if (projectBoard == null) throw new BusinessException(400, "当前无板块");

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
            prompt = "请将对话整理成一段温暖的故事叙述。";
        }
        String content = aiChatService.generateStory(prompt, conversation, boardName);

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
     * 获取指定板块的故事
     */
    public BoardStoryResponse getStory(Long sessionId, Long projectBoardId, Long userId) {
        checkSessionAccess(sessionId, userId);
        return storyRepository.findBySessionIdAndProjectBoardId(sessionId, projectBoardId).map(this::toResponse).orElse(null);
    }

    private BoardStoryResponse toResponse(BoardStory s) {
        String boardCode = null, boardName = null;
        var pbOpt = projectBoardRepository.findById(s.getProjectBoardId());
        if (pbOpt.isPresent()) {
            var bmOpt = boardMetaRepository.findById(pbOpt.get().getBoardMetaId());
            if (bmOpt.isPresent()) { boardCode = bmOpt.get().getCode(); boardName = bmOpt.get().getName(); }
        }
        return BoardStoryResponse.builder().id(s.getId()).sessionId(s.getSessionId()).projectBoardId(s.getProjectBoardId()).boardCode(boardCode).boardName(boardName).content(s.getContent()).versionNo(s.getVersionNo()).createdAt(s.getCreatedAt()).build();
    }
}
