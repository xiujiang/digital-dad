package com.digitaldad.prompt.repository;

import com.digitaldad.prompt.entity.PromptScene;
import com.digitaldad.prompt.enums.PromptRoleType;
import com.digitaldad.prompt.enums.PromptSceneScope;
import com.digitaldad.prompt.enums.PromptStatus;
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
