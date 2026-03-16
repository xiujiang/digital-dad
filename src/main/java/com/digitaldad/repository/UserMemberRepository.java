package com.digitaldad.repository;

import com.digitaldad.entity.UserMember;
import com.digitaldad.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 会员信息 Repository
 */
public interface UserMemberRepository extends JpaRepository<UserMember, Long> {

    Optional<UserMember> findByUserIdAndMemberType(Long userId, String memberType);

    boolean existsByUserIdAndMemberType(Long userId, String memberType);
}
