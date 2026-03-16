package com.digitaldad.repository;

import com.digitaldad.entity.BoardSummary;
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

    /** 某参与者在某项目下的所有小结（用于 my-status 按板块标记是否已生成小结） */
    List<BoardSummary> findByParticipantIdAndProjectId(Long participantId, Long projectId);
}
