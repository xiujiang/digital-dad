package com.digitaldad.prompt.repository;

import com.digitaldad.prompt.entity.PromptSceneItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 场景-模板绑定 Repository
 */
public interface PromptSceneItemRepository extends JpaRepository<PromptSceneItem, Long> {

    List<PromptSceneItem> findBySceneIdOrderByDisplayOrderAsc(Long sceneId);

    boolean existsBySceneIdAndTemplateId(Long sceneId, Long templateId);

    boolean existsByTemplateId(Long templateId);

    void deleteBySceneIdAndTemplateId(Long sceneId, Long templateId);
}
