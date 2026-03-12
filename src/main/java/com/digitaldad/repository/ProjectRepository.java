package com.digitaldad.project.repository;

import com.digitaldad.project.entity.Project;
import com.digitaldad.project.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 项目 Repository
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByIdAndDeletedAtIsNull(Long id);

    Optional<Project> findByShareTokenAndDeletedAtIsNull(String shareToken);

    Page<Project> findByHostUserIdAndDeletedAtIsNull(Long hostUserId, Pageable pageable);

    Page<Project> findByHostUserIdAndDeletedAtIsNullAndStatus(Long hostUserId, ProjectStatus status, Pageable pageable);

    /** 管理员：按主持人、状态、关键词筛选，查询全部项目 */
    @Query("SELECT p FROM Project p WHERE p.deletedAt IS NULL " +
            "AND (:hostUserId IS NULL OR p.hostUserId = :hostUserId) " +
            "AND (:status IS NULL OR p.status = :status) " +
            "AND (:keyword IS NULL OR :keyword = '' OR p.groomName LIKE CONCAT('%', :keyword, '%') " +
            "OR p.brideName LIKE CONCAT('%', :keyword, '%') OR p.projectNo LIKE CONCAT('%', :keyword, '%'))")
    Page<Project> findByFiltersForAdmin(
            @Param("hostUserId") Long hostUserId,
            @Param("status") ProjectStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    boolean existsByProjectNoAndDeletedAtIsNull(String projectNo);

    boolean existsByShareTokenAndDeletedAtIsNull(String shareToken);
}
