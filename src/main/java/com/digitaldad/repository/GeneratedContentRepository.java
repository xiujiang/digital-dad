package com.digitaldad.project.repository;

import com.digitaldad.project.entity.GeneratedContent;
import com.digitaldad.project.enums.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 生成物 Repository
 */
public interface GeneratedContentRepository extends JpaRepository<GeneratedContent, Long> {

    List<GeneratedContent> findByProjectId(Long projectId);

    List<GeneratedContent> findByProjectIdOrderByContentType(Long projectId);

    Optional<GeneratedContent> findByProjectIdAndContentType(Long projectId, ContentType contentType);

    Optional<GeneratedContent> findByProjectIdAndContentTypeAndStatus(Long projectId, ContentType contentType, String status);
}
