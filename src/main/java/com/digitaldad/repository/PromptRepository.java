package com.digitaldad.repository;

import com.digitaldad.entity.Prompt;
import com.digitaldad.enums.PromptContentType;
import com.digitaldad.enums.PromptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 提示词 Repository
 */
public interface PromptRepository extends JpaRepository<Prompt, Long> {

    Optional<Prompt> findByCodeAndIsActiveTrue(String code);

    List<Prompt> findByCodeInAndIsActiveTrue(List<String> codes);

    List<Prompt> findByCodeOrderByVersionNoDesc(String code);

    List<Prompt> findAllByIsActiveTrue();

    List<Prompt> findByIsActiveTrueAndStatus(PromptStatus status);

    List<Prompt> findByIsActiveTrueAndContentTypeAndStatus(PromptContentType contentType, PromptStatus status);

    boolean existsByCode(String code);

    @Modifying
    @Query("UPDATE Prompt p SET p.isActive = false WHERE p.code = :code")
    void deactivateAllByCode(@Param("code") String code);
}
