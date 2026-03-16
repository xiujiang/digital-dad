package com.digitaldad.repository;

import com.digitaldad.entity.UserLoginLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 用户登录流水 Repository
 */
public interface UserLoginLogRepository extends JpaRepository<UserLoginLog, Long> {
}
