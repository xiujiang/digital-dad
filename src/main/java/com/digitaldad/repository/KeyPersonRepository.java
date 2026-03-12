package com.digitaldad.project.repository;

import com.digitaldad.project.entity.KeyPerson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 关键人物 Repository
 */
public interface KeyPersonRepository extends JpaRepository<KeyPerson, Long> {

    List<KeyPerson> findBySessionId(Long sessionId);
}
