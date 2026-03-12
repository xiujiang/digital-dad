package com.digitaldad.project.repository;

import com.digitaldad.project.entity.BoardSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 板块小结 Repository
 */
public interface BoardSummaryRepository extends JpaRepository<BoardSummary, Long> {

    Optional<BoardSummary> findBySessionIdAndProjectBoardId(Long sessionId, Long projectBoardId);

    List<BoardSummary> findByParticipantIdAndProjectBoardIdOrderByVersionNoDesc(
            Long participantId, Long projectBoardId);

    List<BoardSummary> findByParticipantIdOrderByProjectBoardIdAsc(Long participantId);
}
