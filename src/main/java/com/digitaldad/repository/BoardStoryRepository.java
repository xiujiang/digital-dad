package com.digitaldad.project.repository;

import com.digitaldad.project.entity.BoardStory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 故事/时光 Repository
 */
public interface BoardStoryRepository extends JpaRepository<BoardStory, Long> {

    Optional<BoardStory> findBySessionIdAndProjectBoardId(Long sessionId, Long projectBoardId);

    List<BoardStory> findByParticipantIdOrderByProjectBoardIdAsc(Long participantId);
}
