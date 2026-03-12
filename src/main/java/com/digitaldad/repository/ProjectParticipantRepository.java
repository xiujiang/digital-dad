package com.digitaldad.project.repository;

import com.digitaldad.project.entity.ProjectParticipant;
import com.digitaldad.project.enums.ParticipantRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 项目参与者 Repository
 */
public interface ProjectParticipantRepository extends JpaRepository<ProjectParticipant, Long> {

    List<ProjectParticipant> findByProjectId(Long projectId);

    List<ProjectParticipant> findByProjectIdOrderByRoleType(Long projectId);

    Optional<ProjectParticipant> findByProjectIdAndRoleType(Long projectId, ParticipantRole roleType);

    Optional<ProjectParticipant> findByProjectIdAndUserId(Long projectId, Long userId);

    boolean existsByProjectIdAndRoleType(Long projectId, ParticipantRole roleType);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);
}
