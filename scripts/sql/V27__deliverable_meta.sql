-- ============================================================
-- 交付物元数据表（平台级配置）
-- 用于主持人端展示「可生成的交付物类型」列表，与 ContentType 枚举对应
-- ============================================================

CREATE TABLE IF NOT EXISTS `deliverable_meta` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code` VARCHAR(32) NOT NULL COMMENT '唯一编码，与 ContentType 一致: OPENING_SPEECH/GROOM_VOW/BRIDE_VOW',
    `name` VARCHAR(50) NOT NULL COMMENT '显示名称',
    `display_order` INT NOT NULL DEFAULT 0 COMMENT '排序(数字越小越靠前)',
    `description` VARCHAR(200) NULL COMMENT '说明',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED/DISABLED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_display_order` (`display_order`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交付物元数据表';

-- 初始 3 种交付物类型（与 ContentType 枚举一致）
INSERT IGNORE INTO `deliverable_meta` (`code`, `name`, `display_order`, `description`, `status`) VALUES
('OPENING_SPEECH', '婚礼开场白', 1, '根据新郎新娘已确认素材生成婚礼开场白', 'ENABLED'),
('GROOM_VOW', '新郎誓言', 2, '根据新郎已确认素材生成新郎誓言', 'ENABLED'),
('BRIDE_VOW', '新娘誓言', 3, '根据新娘已确认素材生成新娘誓言', 'ENABLED');
