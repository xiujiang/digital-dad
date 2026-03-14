package com.digitaldad.user.repository;

import com.digitaldad.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 用户角色 Repository
 */
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserId(Long userId);

    boolean existsByUserIdAndRole(Long userId, String role);

    void deleteByUserIdAndRole(Long userId, String role);
}
