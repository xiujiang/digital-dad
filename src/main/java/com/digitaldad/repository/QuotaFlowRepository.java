package com.digitaldad.repository;

import com.digitaldad.entity.QuotaFlow;
import com.digitaldad.enums.QuotaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 配额流水 Repository
 */
public interface QuotaFlowRepository extends JpaRepository<QuotaFlow, Long> {

    Page<QuotaFlow> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<QuotaFlow> findByUserIdAndQuotaTypeOrderByCreatedAtDesc(Long userId, QuotaType quotaType, Pageable pageable);
}
