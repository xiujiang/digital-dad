package com.digitaldad.project.repository;

import com.digitaldad.project.entity.SummaryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 小结条目 Repository
 */
public interface SummaryItemRepository extends JpaRepository<SummaryItem, Long> {

    List<SummaryItem> findBySummaryIdOrderByItemOrderAsc(Long summaryId);

    List<SummaryItem> findBySummaryIdAndIsSelectedTrueOrderByItemOrderAsc(Long summaryId);
}
