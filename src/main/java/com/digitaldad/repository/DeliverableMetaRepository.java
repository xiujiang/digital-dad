package com.digitaldad.repository;

import com.digitaldad.entity.DeliverableMeta;
import com.digitaldad.enums.DeliverableMetaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 交付物元数据 Repository
 */
public interface DeliverableMetaRepository extends JpaRepository<DeliverableMeta, Long> {

    Optional<DeliverableMeta> findByCode(String code);

    boolean existsByCode(String code);

    List<DeliverableMeta> findByStatusOrderByDisplayOrderAsc(DeliverableMetaStatus status);
}
