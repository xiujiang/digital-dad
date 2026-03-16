package com.digitaldad.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.dto.UserBoardItemDto;
import com.digitaldad.entity.BoardMeta;
import com.digitaldad.entity.Project;
import com.digitaldad.entity.ProjectBoard;
import com.digitaldad.entity.ProjectParticipant;
import com.digitaldad.enums.ParticipantRole;
import com.digitaldad.enums.ParticipantStatus;
import com.digitaldad.repository.BoardMetaRepository;
import com.digitaldad.repository.ProjectBoardRepository;
import com.digitaldad.repository.ProjectParticipantRepository;
import com.digitaldad.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 项目参与者服务
 * <p>管理用户与项目角色的绑定，同一项目下同一角色只能绑定一人。</p>
 */
@Service
@RequiredArgsConstructor
public class ProjectParticipantService {

    private final ProjectRepository projectRepository;
    private final ProjectParticipantRepository participantRepository;
    private final ProjectBoardRepository projectBoardRepository;
    private final BoardMetaRepository boardMetaRepository;

    /**
     * 绑定用户到项目角色（用户扫码选择身份后调用）
     *
     * @param projectId 项目ID
     * @param userId    用户ID（来自用户模块，如微信用户）
     * @param role      角色（新郎/新娘）
     * @return 绑定后的参与者ID
     */
    @Transactional
    public Long bindParticipant(Long projectId, Long userId, ParticipantRole role) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(404, "项目不存在"));

        // 同一项目下同一角色只能绑定一人
        Optional<ProjectParticipant> existing = participantRepository.findByProjectIdAndRoleType(projectId, role);
        if (existing.isPresent()) {
            if (existing.get().getUserId().equals(userId)) {
                // 同一用户重复绑定，返回已有参与者ID
                return existing.get().getId();
            }
            throw new BusinessException(400, "该身份已被占用，请选择其他身份");
        }

        // 同一用户在同一项目下只能绑定一个角色
        if (participantRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new BusinessException(400, "您已绑定过该项目的身份");
        }

        ProjectParticipant participant = new ProjectParticipant();
        participant.setProjectId(projectId);
        participant.setUserId(userId);
        participant.setRoleType(role);
        participant.setStatus(ParticipantStatus.ENTERED);
        participant.setJoinedAt(LocalDateTime.now());
        participant.setLastActiveAt(LocalDateTime.now());
        participant = participantRepository.save(participant);

        return participant.getId();
    }

    /**
     * 根据项目和用户获取参与者
     */
    public Optional<ProjectParticipant> findByProjectAndUser(Long projectId, Long userId) {
        return participantRepository.findByProjectIdAndUserId(projectId, userId);
    }

    /**
     * 根据ID获取参与者
     */
    public ProjectParticipant getById(Long participantId) {
        return participantRepository.findById(participantId)
                .orElseThrow(() -> new BusinessException(404, "参与者不存在"));
    }

    /**
     * C 端：当前用户参与的所有项目下的板块列表（仅需登录，用于按板块查询故事等）
     * <p>返回该用户作为参与者的每个项目中的全部板块，可用返回的 projectBoardId 调用 GET /api/c/users/me/stories?projectBoardId=xxx。</p>
     *
     * @param userId 当前用户 ID
     * @return 板块列表（projectId、projectBoardId、boardCode、boardName、displayOrder），按 projectId、displayOrder 排序
     */
    public List<UserBoardItemDto> listAllBoardsForUser(Long userId) {
        List<ProjectParticipant> participants = participantRepository.findByUserId(userId);
        if (participants.isEmpty()) {
            return List.of();
        }
        List<Long> projectIds = participants.stream()
                .map(ProjectParticipant::getProjectId)
                .distinct()
                .toList();
        List<ProjectBoard> allBoards = new ArrayList<>();
        for (Long projectId : projectIds) {
            allBoards.addAll(projectBoardRepository.findByProjectIdOrderByDisplayOrderAsc(projectId));
        }
        if (allBoards.isEmpty()) {
            return List.of();
        }
        Set<Long> metaIds = allBoards.stream().map(ProjectBoard::getBoardMetaId).collect(Collectors.toSet());
        Map<Long, BoardMeta> metaMap = new HashMap<>();
        boardMetaRepository.findAllById(metaIds).forEach(m -> metaMap.put(m.getId(), m));
        return allBoards.stream()
                .map(pb -> {
                    BoardMeta meta = metaMap.get(pb.getBoardMetaId());
                    return UserBoardItemDto.builder()
                            .projectId(pb.getProjectId())
                            .projectBoardId(pb.getId())
                            .boardCode(meta != null ? meta.getCode() : null)
                            .boardName(meta != null ? meta.getName() : null)
                            .displayOrder(pb.getDisplayOrder())
                            .build();
                })
                .sorted(Comparator
                        .comparing(UserBoardItemDto::getProjectId)
                        .thenComparing(UserBoardItemDto::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}
