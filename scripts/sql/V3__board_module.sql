-- ============================================================
-- 数字爸爸 v0.1 - 栏目/板块模块表结构
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- 依赖: V2__project_module.sql (project 表)
-- ============================================================
-- 说明：
--   1. board_meta：板块元数据，定义有哪些内容类型（平台级）
--   2. project_board：项目与板块关联，表示该项目下有哪些栏目及顺序（项目级）
--   C 端按 project_board 的顺序展示各板块的聊天内容
-- ============================================================

-- 1. 板块元数据表（平台级配置）
CREATE TABLE IF NOT EXISTS `board_meta` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code` VARCHAR(32) NOT NULL COMMENT '唯一编码，如 FAMILY_ORIGIN/GROWTH/LOVE_STORY/FUTURE_PROMISE',
    `name` VARCHAR(50) NOT NULL COMMENT '显示名称',
    `display_order` INT NOT NULL DEFAULT 0 COMMENT '默认排序(数字越小越靠前)',
    `description` VARCHAR(200) NULL COMMENT '说明',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED/DISABLED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_display_order` (`display_order`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='板块元数据表';

-- 2. 项目板块关联表（项目级：该项目下有哪些栏目）
CREATE TABLE IF NOT EXISTS `project_board` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `board_meta_id` BIGINT NOT NULL COMMENT '板块元数据ID',
    `display_order` INT NOT NULL DEFAULT 0 COMMENT '在该项目中的展示顺序',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_project_board` (`project_id`, `board_meta_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_board_meta_id` (`board_meta_id`),
    CONSTRAINT `fk_project_board_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_project_board_meta` FOREIGN KEY (`board_meta_id`) REFERENCES `board_meta` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目板块关联表';

-- 初始化 4 个板块元数据（v0.1 固定，IGNORE 避免重复执行报错）
INSERT IGNORE INTO `board_meta` (`code`, `name`, `display_order`, `description`, `status`) VALUES
('FAMILY_ORIGIN', '原生家庭', 1, '采访板块：原生家庭相关内容', 'ENABLED'),
('GROWTH', '成长轨迹', 2, '采访板块：成长经历相关内容', 'ENABLED'),
('LOVE_STORY', '爱情故事', 3, '采访板块：爱情故事相关内容', 'ENABLED'),
('FUTURE_PROMISE', '未来承诺', 4, '采访板块：未来承诺相关内容', 'ENABLED');
