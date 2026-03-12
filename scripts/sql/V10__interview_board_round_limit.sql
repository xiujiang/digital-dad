-- ============================================================
-- 板块聊天轮数上限
-- 依赖: V5__session_module.sql, V7__sys_config.sql
-- ============================================================

-- 1. 会话-板块轮数表（按板块统计每会话的对话轮数）
CREATE TABLE IF NOT EXISTS `session_board_rounds` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `session_id` BIGINT NOT NULL COMMENT '会话ID',
    `project_board_id` BIGINT NOT NULL COMMENT '板块ID',
    `round_count` INT NOT NULL DEFAULT 0 COMMENT '该板块在本会话中的对话轮数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_board` (`session_id`, `project_board_id`),
    KEY `idx_session_id` (`session_id`),
    CONSTRAINT `fk_sbr_session` FOREIGN KEY (`session_id`) REFERENCES `interview_session` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_sbr_board` FOREIGN KEY (`project_board_id`) REFERENCES `project_board` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话板块轮数表';

-- 2. 系统配置：板块聊天轮数上限
INSERT INTO `sys_config` (`config_key`, `config_value`, `description`)
VALUES (
    'interview',
    JSON_OBJECT('max_rounds_per_board', 10),
    '采访配置：板块聊天轮数上限'
) ON DUPLICATE KEY UPDATE
    `config_value` = JSON_SET(COALESCE(`config_value`, JSON_OBJECT()), '$.max_rounds_per_board', 10),
    `description` = VALUES(`description`),
    `updated_at` = CURRENT_TIMESTAMP;
