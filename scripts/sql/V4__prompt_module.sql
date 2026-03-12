-- ============================================================
-- 数字爸爸 v0.1 - 提示词模块表结构
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- ============================================================
-- 说明：
--   1. prompt_template：提示词模板（原子单位）
--   2. prompt_version：模板版本，支持多版本、生效、回滚
--   3. prompt_scene：使用场景（如 采访-原生家庭-新郎）
--   4. prompt_scene_item：场景与模板绑定（某场景用哪些模板、顺序）
-- ============================================================

-- 1. 提示词模板表
CREATE TABLE IF NOT EXISTS `prompt_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code` VARCHAR(64) NOT NULL COMMENT '唯一编码',
    `name` VARCHAR(100) NOT NULL COMMENT '显示名称',
    `content_type` VARCHAR(32) NOT NULL DEFAULT 'TEXT' COMMENT '类型: SYSTEM_ROLE/QUESTION/FOLLOW_UP/SUMMARY/GENERATE',
    `description` VARCHAR(500) NULL COMMENT '说明',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED/DISABLED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_content_type` (`content_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提示词模板表';

-- 2. 提示词版本表
CREATE TABLE IF NOT EXISTS `prompt_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `template_id` BIGINT NOT NULL COMMENT '模板ID',
    `version_no` INT NOT NULL COMMENT '版本号',
    `content` TEXT NOT NULL COMMENT '提示词正文',
    `is_active` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否当前生效 0否1是',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT NULL COMMENT '创建人user_id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_version` (`template_id`, `version_no`),
    KEY `idx_template_id` (`template_id`),
    KEY `idx_is_active` (`template_id`, `is_active`),
    CONSTRAINT `fk_version_template` FOREIGN KEY (`template_id`) REFERENCES `prompt_template` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提示词版本表';

-- 3. 提示词使用场景表
CREATE TABLE IF NOT EXISTS `prompt_scene` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code` VARCHAR(64) NOT NULL COMMENT '唯一场景编码',
    `name` VARCHAR(100) NOT NULL COMMENT '显示名称',
    `scope` VARCHAR(32) NOT NULL COMMENT '适用范围: BOARD_INTERVIEW/BOARD_SUMMARY/DELIVERABLE',
    `board_code` VARCHAR(32) NULL COMMENT '关联板块code,如 FAMILY_ORIGIN',
    `role_type` VARCHAR(20) NULL COMMENT '角色: GROOM/BRIDE/COMMON',
    `description` VARCHAR(500) NULL COMMENT '说明',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED/DISABLED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scene_code` (`code`),
    KEY `idx_scope` (`scope`),
    KEY `idx_board_code` (`board_code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提示词使用场景表';

-- 4. 场景-模板绑定表
CREATE TABLE IF NOT EXISTS `prompt_scene_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `scene_id` BIGINT NOT NULL COMMENT '场景ID',
    `template_id` BIGINT NOT NULL COMMENT '模板ID',
    `display_order` INT NOT NULL DEFAULT 0 COMMENT '使用顺序',
    `usage_mode` VARCHAR(20) NOT NULL DEFAULT 'APPEND' COMMENT '使用方式: PREPEND/APPEND/STANDALONE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scene_template` (`scene_id`, `template_id`),
    KEY `idx_scene_id` (`scene_id`),
    KEY `idx_template_id` (`template_id`),
    CONSTRAINT `fk_item_scene` FOREIGN KEY (`scene_id`) REFERENCES `prompt_scene` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_item_template` FOREIGN KEY (`template_id`) REFERENCES `prompt_template` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场景模板绑定表';
