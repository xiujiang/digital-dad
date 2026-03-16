package com.digitaldad.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.dto.BoardInfoDto;
import com.digitaldad.dto.ProjectMyStatusResponse;
import com.digitaldad.entity.BoardMeta;
import com.digitaldad.entity.BoardSummary;
import com.digitaldad.entity.InterviewSession;
import com.digitaldad.entity.ProjectBoard;
import com.digitaldad.entity.ProjectParticipant;
import com.digitaldad.enums.ProjectEntryStep;
import com.digitaldad.enums.SessionStatus;
import com.digitaldad.enums.SummaryStatus;
import com.digitaldad.repository.BoardMetaRepository;
import com.digitaldad.repository.BoardStoryRepository;
import com.digitaldad.repository.BoardSummaryRepository;
import com.digitaldad.repository.InterviewSessionRepository;
import com.digitaldad.repository.ProjectBoardRepository;
import com.digitaldad.repository.ProjectParticipantRepository;
import com.digitaldad.repository.ProjectRepository;
import com.digitaldad.repository.SessionBoardRoundsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * C 端「当前用户在当前项目下的状态」服务
 * <p>根据 projectId + userId 计算 step 及会话/板块/小结等附属信息。</p>
 */
@Service
@RequiredArgsConstructor
public class ProjectParticipantStatusService {

    private final ProjectRepository projectRepository;
    private final ProjectParticipantRepository participantRepository;
    private final InterviewSessionRepository sessionRepository;
    private final BoardSummaryRepository summaryRepository;
    private final BoardStoryRepository storyRepository;
    private final ProjectBoardRepository projectBoardRepository;
    private final BoardMetaRepository boardMetaRepository;
    private final SessionBoardRoundsRepository sessionBoardRoundsRepository;
    private final ConfigService configService;

    private static final List<SessionStatus> ACTIVE_SESSION_STATUSES =
            List.of(SessionStatus.ACTIVE, SessionStatus.WAITING_CONFIRM, SessionStatus.READY);

    /**
     * 获取当前用户在该项目下的状态（绑定、会话、板块、小结等）
     * <p>按板块聚合：当前应进入的板块及其 sessionId（有则返回，无则 sessionId=null，前端带 projectBoardId 调 POST sessions）。</p>
     *
     * @param projectId 项目 ID
     * @param userId    当前用户 ID
     * @return 状态响应，含 step 及该步骤下携带的 ID/信息
     */
    public ProjectMyStatusResponse getMyStatus(Long projectId, Long userId) {
        if (projectRepository.findByIdAndDeletedAtIsNull(projectId).isEmpty()) {
            throw new BusinessException(404, "项目不存在");
        }

        Optional<ProjectParticipant> participantOpt = participantRepository.findByProjectIdAndUserId(projectId, userId);
        if (participantOpt.isEmpty()) {
            List<BoardInfoDto> boardInfos = buildBoardInfoListForProject(projectId, null, null);
            return ProjectMyStatusResponse.builder()
                    .step(ProjectEntryStep.NOT_BOUND)
                    .bound(false)
                    .boards(boardInfos)
                    .build();
        }

        ProjectParticipant participant = participantOpt.get();
        Long participantId = participant.getId();
        List<ProjectBoard> boards = projectBoardRepository.findByProjectIdOrderByDisplayOrderAsc(projectId);
        if (boards.isEmpty()) {
            return buildResponseAllCompleted(participant, null, projectId);
        }

        int currentBoardOrderBase = participant.getCurrentBoardOrder() != null ? participant.getCurrentBoardOrder() : 0;
        Optional<ProjectBoard> currentBoardOpt = boards.stream()
                .filter(b -> b.getDisplayOrder() >= currentBoardOrderBase)
                .min(Comparator.comparingInt(ProjectBoard::getDisplayOrder));

        if (currentBoardOpt.isEmpty()) {
            return buildResponseAllCompleted(participant, null, projectId);
        }

        ProjectBoard currentBoard = currentBoardOpt.get();
        List<InterviewSession> projectSessions = sessionRepository.findByParticipantIdAndProjectIdOrderByCreatedAtAsc(participantId, projectId);
        Map<Long, InterviewSession> boardToSession = projectSessions.stream()
                .collect(Collectors.toMap(InterviewSession::getCurrentProjectBoardId, s -> s, (a, b) -> b));

        InterviewSession sessionForCurrentBoard = boardToSession.get(currentBoard.getId());
        boolean hasActiveSessionForCurrentBoard = sessionForCurrentBoard != null
                && ACTIVE_SESSION_STATUSES.contains(sessionForCurrentBoard.getStatus());

        if (hasActiveSessionForCurrentBoard) {
            return buildResponseWithSession(participant, sessionForCurrentBoard, projectId);
        }

        return buildResponseBoundNoSession(participant, currentBoard, boards, projectId);
    }

    /** 已绑定、当前应进入某板块但该板块尚无进行中会话：返回 currentProjectBoardId，sessionId=null，前端带 projectBoardId 调 POST sessions */
    private ProjectMyStatusResponse buildResponseBoundNoSession(
            ProjectParticipant participant, ProjectBoard currentBoard,
            List<ProjectBoard> boards, Long projectId) {
        Map<Long, BoardMeta> metaMap = boardMetaRepository.findAllById(
                boards.stream().map(ProjectBoard::getBoardMetaId).distinct().collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(BoardMeta::getId, m -> m));
        Map<Long, InterviewSession> boardToSession = sessionRepository
                .findByParticipantIdAndProjectIdOrderByCreatedAtAsc(participant.getId(), projectId)
                .stream().collect(Collectors.toMap(InterviewSession::getCurrentProjectBoardId, s -> s, (a, b) -> b));
        Set<Long> boardIdsWithSummary = summaryRepository.findByParticipantIdAndProjectId(participant.getId(), projectId)
                .stream().map(BoardSummary::getProjectBoardId).collect(Collectors.toSet());
        Set<Long> boardIdsWithStory = storyRepository.findByParticipantIdAndProjectId(participant.getId(), projectId)
                .stream().map(bs -> bs.getProjectBoardId()).collect(Collectors.toSet());
        Integer currentBoardOrder = currentBoard.getDisplayOrder();
        List<BoardInfoDto> boardInfos = boards.stream().map(pb -> {
            BoardMeta bm = metaMap.get(pb.getBoardMetaId());
            boolean completed = participant.getCurrentBoardOrder() != null && pb.getDisplayOrder() < participant.getCurrentBoardOrder();
            InterviewSession sessionForBoard = boardToSession.get(pb.getId());
            return BoardInfoDto.builder()
                    .projectBoardId(pb.getId())
                    .boardCode(bm != null ? bm.getCode() : null)
                    .boardName(bm != null ? bm.getName() : null)
                    .displayOrder(pb.getDisplayOrder())
                    .isCurrent(pb.getId().equals(currentBoard.getId()))
                    .isCompleted(completed)
                    .sessionId(sessionForBoard != null ? sessionForBoard.getId() : null)
                    .hasSummary(boardIdsWithSummary.contains(pb.getId()))
                    .hasStory(boardIdsWithStory.contains(pb.getId()))
                    .build();
        }).collect(Collectors.toList());
        BoardMeta currentMeta = metaMap.get(currentBoard.getBoardMetaId());
        return ProjectMyStatusResponse.builder()
                .step(ProjectEntryStep.BOUND_NO_SESSION)
                .bound(true)
                .role(participant.getRoleType() != null ? participant.getRoleType().name() : null)
                .participantStatus(participant.getStatus() != null ? participant.getStatus().name() : null)
                .sessionId(null)
                .currentProjectBoardId(currentBoard.getId())
                .boardCode(currentMeta != null ? currentMeta.getCode() : null)
                .boardName(currentMeta != null ? currentMeta.getName() : null)
                .currentBoardOrder(currentBoardOrder)
                .boards(boardInfos)
                .maxRoundsPerBoard(configService.getInterviewMaxRoundsPerBoard())
                .build();
    }

    private ProjectMyStatusResponse buildResponseWithSession(ProjectParticipant participant, InterviewSession session, Long projectId) {
        List<ProjectBoard> boards = projectBoardRepository.findByProjectIdOrderByDisplayOrderAsc(projectId);
        if (boards.isEmpty()) {
            return buildResponseAllCompleted(participant, session, projectId);
        }

        Map<Long, BoardMeta> metaMap = boardMetaRepository.findAllById(
                boards.stream().map(ProjectBoard::getBoardMetaId).distinct().collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(BoardMeta::getId, m -> m));
        Map<Long, InterviewSession> boardToSession = sessionRepository
                .findByParticipantIdAndProjectIdOrderByCreatedAtAsc(participant.getId(), projectId)
                .stream().collect(Collectors.toMap(InterviewSession::getCurrentProjectBoardId, s -> s, (a, b) -> b));
        Set<Long> boardIdsWithSummary = summaryRepository.findByParticipantIdAndProjectId(participant.getId(), projectId)
                .stream().map(BoardSummary::getProjectBoardId).collect(Collectors.toSet());
        Set<Long> boardIdsWithStory = storyRepository.findByParticipantIdAndProjectId(participant.getId(), projectId)
                .stream().map(bs -> bs.getProjectBoardId()).collect(Collectors.toSet());

        Long currentBoardId = session.getCurrentProjectBoardId();
        String boardCode = null;
        String boardName = null;
        Integer currentBoardOrder = null;
        if (currentBoardId != null) {
            Optional<ProjectBoard> currentBoard = boards.stream().filter(b -> b.getId().equals(currentBoardId)).findFirst();
            if (currentBoard.isPresent()) {
                BoardMeta bm = metaMap.get(currentBoard.get().getBoardMetaId());
                boardCode = bm != null ? bm.getCode() : null;
                boardName = bm != null ? bm.getName() : null;
                currentBoardOrder = currentBoard.get().getDisplayOrder();
            }
        }

        List<BoardInfoDto> boardInfos = boards.stream().map(pb -> {
            BoardMeta bm = metaMap.get(pb.getBoardMetaId());
            boolean completed = session.getStatus() == SessionStatus.COMPLETED
                    || (participant.getCurrentBoardOrder() != null && pb.getDisplayOrder() < participant.getCurrentBoardOrder());
            InterviewSession sessionForBoard = boardToSession.get(pb.getId());
            return BoardInfoDto.builder()
                    .projectBoardId(pb.getId())
                    .boardCode(bm != null ? bm.getCode() : null)
                    .boardName(bm != null ? bm.getName() : null)
                    .displayOrder(pb.getDisplayOrder())
                    .isCurrent(pb.getId().equals(currentBoardId))
                    .isCompleted(completed)
                    .sessionId(sessionForBoard != null ? sessionForBoard.getId() : null)
                    .hasSummary(boardIdsWithSummary.contains(pb.getId()))
                    .hasStory(boardIdsWithStory.contains(pb.getId()))
                    .build();
        }).collect(Collectors.toList());

        int currentBoardRoundCount = 0;
        int maxRounds = configService.getInterviewMaxRoundsPerBoard();
        if (currentBoardId != null) {
            currentBoardRoundCount = sessionBoardRoundsRepository
                    .findBySessionIdAndProjectBoardId(session.getId(), currentBoardId)
                    .map(r -> r.getRoundCount() != null ? r.getRoundCount() : 0)
                    .orElse(0);
        }

        // 当前板块是否有待确认的小结
        Long currentSummaryId = null;
        String currentSummaryStatus = null;
        if (currentBoardId != null) {
            Optional<BoardSummary> summaryOpt = summaryRepository.findBySessionIdAndProjectBoardId(session.getId(), currentBoardId);
            if (summaryOpt.isPresent() && summaryOpt.get().getStatus() == SummaryStatus.WAITING_CONFIRM) {
                currentSummaryId = summaryOpt.get().getId();
                currentSummaryStatus = SummaryStatus.WAITING_CONFIRM.name();
            }
        }

        ProjectEntryStep step = (currentSummaryId != null)
                ? ProjectEntryStep.WAITING_SUMMARY_CONFIRM
                : ProjectEntryStep.IN_CHAT;

        return ProjectMyStatusResponse.builder()
                .step(step)
                .bound(true)
                .role(participant.getRoleType() != null ? participant.getRoleType().name() : null)
                .participantStatus(participant.getStatus() != null ? participant.getStatus().name() : null)
                .sessionId(session.getId())
                .sessionStatus(session.getStatus().name())
                .currentProjectBoardId(currentBoardId)
                .boardCode(boardCode)
                .boardName(boardName)
                .currentBoardOrder(currentBoardOrder)
                .boards(boardInfos)
                .currentBoardRoundCount(currentBoardRoundCount)
                .maxRoundsPerBoard(maxRounds)
                .currentSummaryId(currentSummaryId)
                .currentSummaryStatus(currentSummaryStatus)
                .build();
    }

    /** 仅根据项目构建板块简要列表（无参与者时 currentBoardId、participantId 传 null，无 session/summary/story 等） */
    private List<BoardInfoDto> buildBoardInfoListForProject(Long projectId, Long currentBoardId, Long participantId) {
        List<ProjectBoard> boardList = projectBoardRepository.findByProjectIdOrderByDisplayOrderAsc(projectId);
        if (boardList.isEmpty()) {
            return List.of();
        }
        Map<Long, BoardMeta> metaMap = boardMetaRepository.findAllById(
                boardList.stream().map(ProjectBoard::getBoardMetaId).distinct().collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(BoardMeta::getId, m -> m));
        Map<Long, InterviewSession> boardToSession = participantId != null
                ? sessionRepository.findByParticipantIdAndProjectIdOrderByCreatedAtAsc(participantId, projectId)
                        .stream().collect(Collectors.toMap(InterviewSession::getCurrentProjectBoardId, s -> s, (a, b) -> b))
                : Map.of();
        Set<Long> boardIdsWithSummary = participantId != null
                ? summaryRepository.findByParticipantIdAndProjectId(participantId, projectId)
                        .stream().map(BoardSummary::getProjectBoardId).collect(Collectors.toSet())
                : Set.of();
        Set<Long> boardIdsWithStory = participantId != null
                ? storyRepository.findByParticipantIdAndProjectId(participantId, projectId)
                        .stream().map(bs -> bs.getProjectBoardId()).collect(Collectors.toSet())
                : Set.of();
        return boardList.stream().map(pb -> {
            BoardMeta bm = metaMap.get(pb.getBoardMetaId());
            InterviewSession sessionForBoard = boardToSession.get(pb.getId());
            return BoardInfoDto.builder()
                    .projectBoardId(pb.getId())
                    .boardCode(bm != null ? bm.getCode() : null)
                    .boardName(bm != null ? bm.getName() : null)
                    .displayOrder(pb.getDisplayOrder())
                    .isCurrent(currentBoardId != null && pb.getId().equals(currentBoardId))
                    .isCompleted(false)
                    .sessionId(sessionForBoard != null ? sessionForBoard.getId() : null)
                    .hasSummary(boardIdsWithSummary.contains(pb.getId()))
                    .hasStory(boardIdsWithStory.contains(pb.getId()))
                    .build();
        }).collect(Collectors.toList());
    }

    private ProjectMyStatusResponse buildResponseAllCompleted(ProjectParticipant participant, InterviewSession sessionOrNull, Long projectId) {
        List<BoardInfoDto> boards = List.of();
        if (projectId != null) {
            List<ProjectBoard> boardList = projectBoardRepository.findByProjectIdOrderByDisplayOrderAsc(projectId);
            Map<Long, BoardMeta> metaMap = boardMetaRepository.findAllById(
                    boardList.stream().map(ProjectBoard::getBoardMetaId).distinct().collect(Collectors.toList())
            ).stream().collect(Collectors.toMap(BoardMeta::getId, m -> m));
            Map<Long, InterviewSession> boardToSession = sessionRepository
                    .findByParticipantIdAndProjectIdOrderByCreatedAtAsc(participant.getId(), projectId)
                    .stream().collect(Collectors.toMap(InterviewSession::getCurrentProjectBoardId, s -> s, (a, b) -> b));
            Set<Long> boardIdsWithSummary = summaryRepository.findByParticipantIdAndProjectId(participant.getId(), projectId)
                    .stream().map(BoardSummary::getProjectBoardId).collect(Collectors.toSet());
            Set<Long> boardIdsWithStory = storyRepository.findByParticipantIdAndProjectId(participant.getId(), projectId)
                    .stream().map(bs -> bs.getProjectBoardId()).collect(Collectors.toSet());
            Long currentBoardId = sessionOrNull != null ? sessionOrNull.getCurrentProjectBoardId() : null;
            boards = boardList.stream().map(pb -> {
                BoardMeta bm = metaMap.get(pb.getBoardMetaId());
                InterviewSession sessionForBoard = boardToSession.get(pb.getId());
                return BoardInfoDto.builder()
                        .projectBoardId(pb.getId())
                        .boardCode(bm != null ? bm.getCode() : null)
                        .boardName(bm != null ? bm.getName() : null)
                        .displayOrder(pb.getDisplayOrder())
                        .isCurrent(pb.getId().equals(currentBoardId))
                        .isCompleted(true)
                        .sessionId(sessionForBoard != null ? sessionForBoard.getId() : null)
                        .hasSummary(boardIdsWithSummary.contains(pb.getId()))
                        .hasStory(boardIdsWithStory.contains(pb.getId()))
                        .build();
            }).collect(Collectors.toList());
        }

        return ProjectMyStatusResponse.builder()
                .step(ProjectEntryStep.ALL_COMPLETED)
                .bound(true)
                .role(participant.getRoleType() != null ? participant.getRoleType().name() : null)
                .participantStatus(participant.getStatus() != null ? participant.getStatus().name() : null)
                .sessionId(sessionOrNull != null ? sessionOrNull.getId() : null)
                .sessionStatus(sessionOrNull != null ? sessionOrNull.getStatus().name() : null)
                .boards(boards)
                .build();
    }
}
