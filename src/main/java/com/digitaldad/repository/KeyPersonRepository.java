package com.digitaldad.repository;

import com.digitaldad.entity.KeyPerson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 关键人物 Repository
 */
public interface KeyPersonRepository extends JpaRepository<KeyPerson, Long> {

    List<KeyPerson> findBySessionId(Long sessionId);

    List<KeyPerson> findByUserIdOrderByCreatedAtAsc(Long userId);

    /** 按用户+角色标签查，用于故事列表「按角色」筛选 */
    List<KeyPerson> findByUserIdAndRoleLabel(Long userId, String roleLabel);
}
