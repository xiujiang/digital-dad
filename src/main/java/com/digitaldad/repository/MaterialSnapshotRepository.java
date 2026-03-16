package com.digitaldad.repository;

import com.digitaldad.entity.MaterialSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 素材快照 Repository
 */
public interface MaterialSnapshotRepository extends JpaRepository<MaterialSnapshot, Long> {

    List<MaterialSnapshot> findByProjectIdAndParticipantIdOrderByProjectBoardIdAsc(
            Long projectId, Long participantId);

    List<MaterialSnapshot> findByProjectId(Long projectId);
}
