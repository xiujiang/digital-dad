-- ============================================================
-- 板块元数据初始化（4 个采访板块：code + 中文 name）
-- 依赖: V3__board_module.sql（board_meta 表已存在）
-- 可重复执行：已存在则按 code 更新 name/display_order/description/status
-- ============================================================

INSERT INTO `board_meta` (`code`, `name`, `display_order`, `description`, `status`, `created_at`, `updated_at`)
VALUES
('FAMILY_ORIGIN', '原生家庭', 1, '采访板块：原生家庭相关内容', 'ENABLED', NOW(), NOW()),
('GROWTH', '成长轨迹', 2, '采访板块：成长经历相关内容', 'ENABLED', NOW(), NOW()),
('LOVE_STORY', '爱情故事', 3, '采访板块：爱情故事相关内容', 'ENABLED', NOW(), NOW()),
('FUTURE_PROMISE', '未来承诺', 4, '采访板块：未来承诺相关内容', 'ENABLED', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `display_order` = VALUES(`display_order`),
  `description` = VALUES(`description`),
  `status` = VALUES(`status`),
  `updated_at` = NOW();
