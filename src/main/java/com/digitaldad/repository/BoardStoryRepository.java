package com.digitaldad.repository;

import com.digitaldad.entity.BoardStory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

/**
 * 故事/时光 Repository
 */
public interface BoardStoryRepository extends JpaRepository<BoardStory, Long> {

    Optional<BoardStory> findBySessionIdAndProjectBoardId(Long sessionId, Long projectBoardId);

    List<BoardStory> findByParticipantIdOrderByProjectBoardIdAsc(Long participantId);

    /** 某参与者在某项目下的所有故事（用于 my-status 按板块标记是否已生成故事） */
    List<BoardStory> findByParticipantIdAndProjectId(Long participantId, Long projectId);

    /** 按多个参与者查故事，用于 C 端「我的全部故事」 */
    List<BoardStory> findByParticipantIdIn(List<Long> participantIds, Sort sort);

    /** C 端「我的板块」：当前用户有故事的项目板块 ID 去重 */
    @Query("SELECT DISTINCT s.projectBoardId FROM BoardStory s WHERE s.participantId IN :participantIds")
    List<Long> findDistinctProjectBoardIdsByParticipantIdIn(@Param("participantIds") List<Long> participantIds);
}
