package com.digitaldad.repository;

import com.digitaldad.entity.BoardMeta;
import com.digitaldad.enums.BoardMetaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 板块元数据 Repository
 */
public interface BoardMetaRepository extends JpaRepository<BoardMeta, Long> {

    Optional<BoardMeta> findByCode(String code);

    boolean existsByCode(String code);

    List<BoardMeta> findByStatusOrderByDisplayOrderAsc(BoardMetaStatus status);

    /** 按 code 列表查询并按 display_order 排序（用于新项目挂载默认板块） */
    List<BoardMeta> findByCodeInOrderByDisplayOrderAsc(Collection<String> codes);
}
