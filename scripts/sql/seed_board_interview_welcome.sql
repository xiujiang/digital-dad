-- ============================================================
-- 板块采访欢迎语：提示词 + 场景 + 场景项
-- 用于创建会话时首条 AI 消息，支持变量 {{boardName}}
-- 依赖: prompt / prompt_scene / prompt_scene_item 表已存在
-- ============================================================

-- 1. 提示词表：欢迎语模板（运行时将 {{boardName}} 替换为当前板块名称）
INSERT INTO `prompt` (`code`, `name`, `content_type`, `description`, `status`, `version_no`, `content`, `is_active`, `created_at`, `updated_at`)
VALUES (
  'BOARD_INTERVIEW_WELCOME',
  '板块采访欢迎语',
  'TEXT',
  '创建会话时首条 AI 欢迎语，变量 {{boardName}} 由业务按当前板块名称替换',
  'ENABLED',
  1,
  '我是{{boardName}}板块，我们来一起聊聊你的{{boardName}}方面的事情，我们可以开始了。',
  1,
  NOW(),
  NOW()
) ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `content_type` = VALUES(`content_type`),
  `description` = VALUES(`description`),
  `content` = VALUES(`content`),
  `updated_at` = NOW();

-- 2. 场景表：板块采访欢迎语（通用，不限定 board_code，按会话当前板块取名称）
INSERT IGNORE INTO `prompt_scene` (`code`, `name`, `scope`, `board_code`, `role_type`, `description`, `status`, `created_at`, `updated_at`)
VALUES (
  'BOARD_INTERVIEW_WELCOME',
  '板块采访欢迎语',
  'BOARD_INTERVIEW',
  NULL,
  NULL,
  '创建会话时首条 AI 欢迎语，使用提示词 BOARD_INTERVIEW_WELCOME，替换 {{boardName}}',
  'ENABLED',
  NOW(),
  NOW()
);

-- 3. 场景项：上述场景绑定提示词
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'BOARD_INTERVIEW_WELCOME', 0, 'STANDALONE', NOW()
FROM `prompt_scene` s
WHERE s.`code` = 'BOARD_INTERVIEW_WELCOME'
LIMIT 1;
