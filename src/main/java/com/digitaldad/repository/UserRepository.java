package com.digitaldad.repository;

import com.digitaldad.entity.User;
import com.digitaldad.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 用户 Repository
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhoneAndDeletedAtIsNull(String phone);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByPhoneAndDeletedAtIsNull(String phone);

    /** 按角色筛选用户（存在该角色记录的用户） */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL " +
            "AND EXISTS (SELECT 1 FROM UserRole r WHERE r.userId = u.id AND r.role = :role) " +
            "AND (:status IS NULL OR u.status = :status) " +
            "AND (:keyword IS NULL OR :keyword = '' OR u.name LIKE CONCAT('%', :keyword, '%') OR u.phone LIKE CONCAT('%', :keyword, '%'))")
    Page<User> findByRoleAndFilters(
            @Param("role") String role,
            @Param("status") UserStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);
}
