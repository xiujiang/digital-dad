package com.digitaldad.prompt.repository;

import com.digitaldad.prompt.entity.PromptTemplate;
import com.digitaldad.prompt.enums.PromptContentType;
import com.digitaldad.prompt.enums.PromptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 提示词模板 Repository
 */
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {

    Optional<PromptTemplate> findByCode(String code);

    boolean existsByCode(String code);

    List<PromptTemplate> findByStatus(PromptStatus status);

    List<PromptTemplate> findByContentTypeAndStatus(PromptContentType contentType, PromptStatus status);
}
