package com.digitaldad.repository;

import com.digitaldad.entity.SessionBoardRounds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

/**
 * 会话板块轮数 Repository
 */
public interface SessionBoardRoundsRepository extends JpaRepository<SessionBoardRounds, Long> {

    Optional<SessionBoardRounds> findBySessionIdAndProjectBoardId(Long sessionId, Long projectBoardId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM SessionBoardRounds r WHERE r.sessionId = :sessionId AND r.projectBoardId = :projectBoardId")
    Optional<SessionBoardRounds> findBySessionIdAndProjectBoardIdForUpdate(
            @Param("sessionId") Long sessionId,
            @Param("projectBoardId") Long projectBoardId);
}
