package com.digitaldad.user.repository;

import com.digitaldad.user.entity.UserAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 超管 Repository
 */
public interface UserAdminRepository extends JpaRepository<UserAdmin, Long> {

    Optional<UserAdmin> findByAccount(String account);

    Optional<UserAdmin> findByUserId(Long userId);

    boolean existsByAccount(String account);
}
