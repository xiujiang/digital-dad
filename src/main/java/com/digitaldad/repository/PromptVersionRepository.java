package com.digitaldad.prompt.repository;

import com.digitaldad.prompt.entity.PromptVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 提示词版本 Repository
 */
public interface PromptVersionRepository extends JpaRepository<PromptVersion, Long> {

    List<PromptVersion> findByTemplateIdOrderByVersionNoDesc(Long templateId);

    Optional<PromptVersion> findByTemplateIdAndVersionNo(Long templateId, Integer versionNo);

    Optional<PromptVersion> findByTemplateIdAndIsActiveTrue(Long templateId);

    boolean existsByTemplateIdAndVersionNo(Long templateId, Integer versionNo);

    @Modifying
    @Query("UPDATE PromptVersion v SET v.isActive = false WHERE v.templateId = :templateId")
    void deactivateAllByTemplateId(@Param("templateId") Long templateId);
}
