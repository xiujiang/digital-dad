package com.digitaldad.user.repository;

import com.digitaldad.user.entity.UserMember;
import com.digitaldad.user.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 会员信息 Repository
 */
public interface UserMemberRepository extends JpaRepository<UserMember, Long> {

    Optional<UserMember> findByUserIdAndMemberType(Long userId, String memberType);

    boolean existsByUserIdAndMemberType(Long userId, String memberType);
}
