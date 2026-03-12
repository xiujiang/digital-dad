-- ============================================================
-- 通用配置表（方案三：JSON 配置块）
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- ============================================================

CREATE TABLE IF NOT EXISTS `sys_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_key` VARCHAR(100) NOT NULL COMMENT '配置键，唯一',
    `config_value` JSON NULL COMMENT '配置值（JSON）',
    `description` VARCHAR(200) NULL COMMENT '描述',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 初始化：会员套餐配置
INSERT INTO `sys_config` (`config_key`, `config_value`, `description`)
VALUES (
    'member.packages',
    JSON_OBJECT(
        'annual', JSON_OBJECT(
            'name', '年费会员',
            'quota', 60,
            'valid_days', 365
        ),
        'single', JSON_OBJECT(
            'name', '单次会员',
            'quota', 1,
            'valid_days', NULL
        )
    ),
    '会员套餐配置：annual 年费会员、single 单次会员'
) ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`), `description` = VALUES(`description`), `updated_at` = CURRENT_TIMESTAMP;
