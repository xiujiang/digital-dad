package com.digitaldad.board.repository;

import com.digitaldad.board.entity.ProjectBoard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 项目板块关联 Repository
 */
public interface ProjectBoardRepository extends JpaRepository<ProjectBoard, Long> {

    List<ProjectBoard> findByProjectIdOrderByDisplayOrderAsc(Long projectId);

    Optional<ProjectBoard> findByProjectIdAndBoardMetaId(Long projectId, Long boardMetaId);

    boolean existsByProjectIdAndBoardMetaId(Long projectId, Long boardMetaId);

    void deleteByProjectIdAndBoardMetaId(Long projectId, Long boardMetaId);
}
