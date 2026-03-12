package com.digitaldad.user.repository;

import com.digitaldad.user.entity.QuotaFlow;
import com.digitaldad.user.enums.QuotaType;
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
