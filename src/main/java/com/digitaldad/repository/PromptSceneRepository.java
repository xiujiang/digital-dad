package com.digitaldad.repository;

import com.digitaldad.entity.PromptScene;
import com.digitaldad.enums.PromptRoleType;
import com.digitaldad.enums.PromptSceneScope;
import com.digitaldad.enums.PromptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 提示词场景 Repository
 */
public interface PromptSceneRepository extends JpaRepository<PromptScene, Long> {

    Optional<PromptScene> findByCode(String code);

    boolean existsByCode(String code);

    List<PromptScene> findByStatus(PromptStatus status);

    List<PromptScene> findByScopeAndStatus(PromptSceneScope scope, PromptStatus status);

    List<PromptScene> findByScopeAndBoardCodeAndStatus(PromptSceneScope scope, String boardCode, PromptStatus status);

    List<PromptScene> findByScopeAndBoardCodeAndRoleTypeAndStatus(
            PromptSceneScope scope, String boardCode, PromptRoleType roleType, PromptStatus status);
}
