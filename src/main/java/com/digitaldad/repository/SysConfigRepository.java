package com.digitaldad.config.repository;

import com.digitaldad.config.entity.SysConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 系统配置 Repository
 */
public interface SysConfigRepository extends JpaRepository<SysConfig, Long> {

    Optional<SysConfig> findByConfigKey(String configKey);
}
