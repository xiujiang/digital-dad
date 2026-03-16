-- ============================================================
-- 登录流水表 + 微信 session_key 字段
-- 依赖: V1__user_module.sql
-- ============================================================

-- 1. 用户登录流水表（审计：谁、何时、何种方式登录）
CREATE TABLE IF NOT EXISTS `user_login_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `login_at` DATETIME NOT NULL COMMENT '登录时间',
    `channel` VARCHAR(32) NOT NULL COMMENT '登录渠道: WECHAT_MINIPROGRAM/PHONE_CODE/PASSWORD/ADMIN',
    `ip` VARCHAR(64) NULL COMMENT '请求IP',
    `user_agent` VARCHAR(500) NULL COMMENT 'User-Agent',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_login_at` (`login_at`),
    KEY `idx_channel` (`channel`),
    CONSTRAINT `fk_login_log_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户登录流水表';

-- 2. 微信用户表增加 session_key（用于解密手机号等敏感数据）
ALTER TABLE `user_wechat` ADD COLUMN `session_key` VARCHAR(128) NULL COMMENT '微信 session_key，用于解密' AFTER `city`;
