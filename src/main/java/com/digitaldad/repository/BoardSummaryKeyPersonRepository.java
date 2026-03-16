package com.digitaldad.repository;

import com.digitaldad.entity.BoardSummaryKeyPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * 小结与关键人物绑定 Repository
 */
public interface BoardSummaryKeyPersonRepository extends JpaRepository<BoardSummaryKeyPerson, Long> {

    List<BoardSummaryKeyPerson> findBySummaryIdOrderByIdAsc(Long summaryId);

    @Modifying
    @Query("DELETE FROM BoardSummaryKeyPerson b WHERE b.summaryId = ?1")
    void deleteBySummaryId(Long summaryId);
}
