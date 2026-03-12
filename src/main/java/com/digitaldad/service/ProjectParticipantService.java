package com.digitaldad.project.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.project.entity.Project;
import com.digitaldad.project.entity.ProjectParticipant;
import com.digitaldad.project.enums.ParticipantRole;
import com.digitaldad.project.enums.ParticipantStatus;
import com.digitaldad.project.repository.ProjectParticipantRepository;
import com.digitaldad.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 项目参与者服务
 * <p>管理用户与项目角色的绑定，同一项目下同一角色只能绑定一人。</p>
 */
@Service
@RequiredArgsConstructor
public class ProjectParticipantService {

    private final ProjectRepository projectRepository;
    private final ProjectParticipantRepository participantRepository;

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
}
