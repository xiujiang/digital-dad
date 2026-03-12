package com.digitaldad.user.repository;

import com.digitaldad.user.entity.User;
import com.digitaldad.user.enums.UserStatus;
import com.digitaldad.user.enums.UserType;
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

    Optional<User> findByPhoneAndUserTypeAndDeletedAtIsNull(String phone, UserType userType);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByPhoneAndDeletedAtIsNull(String phone);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND u.userType = :userType " +
            "AND (:status IS NULL OR u.status = :status) " +
            "AND (:keyword IS NULL OR :keyword = '' OR u.name LIKE CONCAT('%', :keyword, '%') OR u.phone LIKE CONCAT('%', :keyword, '%'))")
    Page<User> findByUserTypeAndFilters(
            @Param("userType") UserType userType,
            @Param("status") UserStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);
}
