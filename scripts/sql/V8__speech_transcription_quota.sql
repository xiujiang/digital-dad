-- ============================================================
-- 语音转写配额模块
-- 依赖: V1__user_module.sql
-- ============================================================

-- 1. 语音转写使用记录表
CREATE TABLE IF NOT EXISTS `speech_transcription_usage` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `duration_seconds` INT NOT NULL DEFAULT 0 COMMENT '本次实际时长（秒）',
    `deducted_seconds` INT NOT NULL DEFAULT 0 COMMENT '本次实际扣减额（秒）',
    `remaining_after` INT NOT NULL DEFAULT 0 COMMENT '扣减后剩余额度（秒）',
    `connect_id` VARCHAR(64) NULL COMMENT 'WebSocket connectId，用于追溯',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_user_created` (`user_id`, `created_at`),
    CONSTRAINT `fk_stu_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语音转写使用记录表';

-- 2. 系统配置：语音转写默认配额
INSERT INTO `sys_config` (`config_key`, `config_value`, `description`)
VALUES (
    'speech.transcription',
    JSON_OBJECT('default_seconds', 3600),
    '语音转写默认配额（秒），首次使用时懒加载初始化'
) ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`), `description` = VALUES(`description`), `updated_at` = CURRENT_TIMESTAMP;
