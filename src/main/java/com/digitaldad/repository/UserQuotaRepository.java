package com.digitaldad.user.repository;

import com.digitaldad.user.entity.UserQuota;
import com.digitaldad.user.enums.QuotaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

/**
 * 配额 Repository
 */
public interface UserQuotaRepository extends JpaRepository<UserQuota, Long> {

    Optional<UserQuota> findByUserIdAndQuotaType(Long userId, QuotaType quotaType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM UserQuota q WHERE q.userId = :userId AND q.quotaType = :quotaType")
    Optional<UserQuota> findByUserIdAndQuotaTypeForUpdate(
            @Param("userId") Long userId,
            @Param("quotaType") QuotaType quotaType);
}
