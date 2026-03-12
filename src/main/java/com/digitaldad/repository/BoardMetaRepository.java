package com.digitaldad.board.repository;

import com.digitaldad.board.entity.BoardMeta;
import com.digitaldad.board.enums.BoardMetaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 板块元数据 Repository
 */
public interface BoardMetaRepository extends JpaRepository<BoardMeta, Long> {

    Optional<BoardMeta> findByCode(String code);

    boolean existsByCode(String code);

    List<BoardMeta> findByStatusOrderByDisplayOrderAsc(BoardMetaStatus status);
}
