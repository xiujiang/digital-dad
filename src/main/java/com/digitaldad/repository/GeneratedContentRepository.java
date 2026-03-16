package com.digitaldad.repository;

import com.digitaldad.entity.GeneratedContent;
import com.digitaldad.enums.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    /** 今日生成次数 */
    @Query("SELECT COUNT(g) FROM GeneratedContent g WHERE g.createdAt >= :start AND g.createdAt < :end")
    long countByCreatedAtBetween(
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 待处理内容数（status = OUTDATED） */
    long countByStatus(com.digitaldad.enums.ContentStatus status);

    /** 管理员：分页列出全部交付物（仅项目未删除的） */
    @Query("SELECT g FROM GeneratedContent g WHERE EXISTS (SELECT 1 FROM Project p WHERE p.id = g.projectId AND p.deletedAt IS NULL)")
    org.springframework.data.domain.Page<GeneratedContent> findAllByProjectNotDeleted(org.springframework.data.domain.Pageable pageable);
}
