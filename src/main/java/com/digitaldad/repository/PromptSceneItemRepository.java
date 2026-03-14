package com.digitaldad.prompt.repository;

import com.digitaldad.prompt.entity.PromptSceneItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 场景-提示词绑定 Repository
 */
public interface PromptSceneItemRepository extends JpaRepository<PromptSceneItem, Long> {

    List<PromptSceneItem> findBySceneIdOrderByDisplayOrderAsc(Long sceneId);

    boolean existsBySceneIdAndPromptCode(Long sceneId, String promptCode);

    boolean existsByPromptCode(String promptCode);
}
