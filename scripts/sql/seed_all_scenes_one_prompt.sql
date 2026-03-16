-- ============================================================
-- 1. 全部场景（18 个） + 每个场景 1 份提示词
-- 可重复执行：场景 INSERT IGNORE；场景项 INSERT IGNORE（依赖 scene_id + prompt_code 唯一）
-- ============================================================

-- 1) 通用采访用提示词（若不存在则插入）
INSERT IGNORE INTO `prompt` (`code`, `name`, `content_type`, `description`, `status`, `version_no`, `content`, `is_active`, `created_at`, `updated_at`)
VALUES ('INTERVIEW_COMMON_SYSTEM', '通用采访-系统角色', 'SYSTEM_ROLE', '通用板块采访时的系统角色', 'ENABLED', 1,
 '你是一位善于倾听的婚礼采访助手。请用温和、开放的方式引导对方分享故事，避免审问感。', 1, NOW(), NOW());

-- 2) 全部场景（18 个）
INSERT IGNORE INTO `prompt_scene` (`code`, `name`, `scope`, `board_code`, `role_type`, `description`, `status`, `created_at`, `updated_at`)
VALUES
('DELIVERABLE_OPENING_SPEECH', '婚礼开场白', 'DELIVERABLE', NULL, NULL, '生成婚礼开场白文案', 'ENABLED', NOW(), NOW()),
('DELIVERABLE_GROOM_VOW', '新郎誓言', 'DELIVERABLE', NULL, NULL, '生成新郎誓言', 'ENABLED', NOW(), NOW()),
('DELIVERABLE_BRIDE_VOW', '新娘誓言', 'DELIVERABLE', NULL, NULL, '生成新娘誓言', 'ENABLED', NOW(), NOW()),
('BOARD_SUMMARY_COMMON', '通用板块小结', 'BOARD_SUMMARY', NULL, NULL, '无板块时的兜底小结场景', 'ENABLED', NOW(), NOW()),
('BOARD_SUMMARY_FAMILY_ORIGIN', '原生家庭·板块小结', 'BOARD_SUMMARY', 'FAMILY_ORIGIN', NULL, '原生家庭板块小结', 'ENABLED', NOW(), NOW()),
('BOARD_SUMMARY_GROWTH', '成长轨迹·板块小结', 'BOARD_SUMMARY', 'GROWTH', NULL, '成长轨迹板块小结', 'ENABLED', NOW(), NOW()),
('BOARD_SUMMARY_LOVE_STORY', '爱情故事·板块小结', 'BOARD_SUMMARY', 'LOVE_STORY', NULL, '爱情故事板块小结', 'ENABLED', NOW(), NOW()),
('BOARD_SUMMARY_FUTURE_PROMISE', '未来承诺·板块小结', 'BOARD_SUMMARY', 'FUTURE_PROMISE', NULL, '未来承诺板块小结', 'ENABLED', NOW(), NOW()),
('BOARD_INTERVIEW_FAMILY_ORIGIN_GROOM', '采访-原生家庭-新郎', 'BOARD_INTERVIEW', 'FAMILY_ORIGIN', 'GROOM', '新郎侧原生家庭采访', 'ENABLED', NOW(), NOW()),
('BOARD_INTERVIEW_FAMILY_ORIGIN_BRIDE', '采访-原生家庭-新娘', 'BOARD_INTERVIEW', 'FAMILY_ORIGIN', 'BRIDE', '新娘侧原生家庭采访', 'ENABLED', NOW(), NOW()),
('BOARD_INTERVIEW_GROWTH_GROOM', '采访-成长轨迹-新郎', 'BOARD_INTERVIEW', 'GROWTH', 'GROOM', '新郎侧成长轨迹采访', 'ENABLED', NOW(), NOW()),
('BOARD_INTERVIEW_GROWTH_BRIDE', '采访-成长轨迹-新娘', 'BOARD_INTERVIEW', 'GROWTH', 'BRIDE', '新娘侧成长轨迹采访', 'ENABLED', NOW(), NOW()),
('BOARD_INTERVIEW_LOVE_STORY_GROOM', '采访-爱情故事-新郎', 'BOARD_INTERVIEW', 'LOVE_STORY', 'GROOM', '新郎侧爱情故事采访', 'ENABLED', NOW(), NOW()),
('BOARD_INTERVIEW_LOVE_STORY_BRIDE', '采访-爱情故事-新娘', 'BOARD_INTERVIEW', 'LOVE_STORY', 'BRIDE', '新娘侧爱情故事采访', 'ENABLED', NOW(), NOW()),
('BOARD_INTERVIEW_FUTURE_PROMISE_GROOM', '采访-未来承诺-新郎', 'BOARD_INTERVIEW', 'FUTURE_PROMISE', 'GROOM', '新郎侧未来承诺采访', 'ENABLED', NOW(), NOW()),
('BOARD_INTERVIEW_FUTURE_PROMISE_BRIDE', '采访-未来承诺-新娘', 'BOARD_INTERVIEW', 'FUTURE_PROMISE', 'BRIDE', '新娘侧未来承诺采访', 'ENABLED', NOW(), NOW()),
('BOARD_INTERVIEW_COMMON_GROOM', '采访-通用-新郎', 'BOARD_INTERVIEW', NULL, 'GROOM', '新郎侧通用采访', 'ENABLED', NOW(), NOW()),
('BOARD_INTERVIEW_COMMON_BRIDE', '采访-通用-新娘', 'BOARD_INTERVIEW', NULL, 'BRIDE', '新娘侧通用采访', 'ENABLED', NOW(), NOW());

-- 3) 每个场景 1 份提示词（18 条场景项）
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'OPENING_SPEECH_MAIN', 0, 'APPEND', NOW() FROM `prompt_scene` s WHERE s.`code` = 'DELIVERABLE_OPENING_SPEECH' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'GROOM_VOW_MAIN', 0, 'APPEND', NOW() FROM `prompt_scene` s WHERE s.`code` = 'DELIVERABLE_GROOM_VOW' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'BRIDE_VOW_MAIN', 0, 'APPEND', NOW() FROM `prompt_scene` s WHERE s.`code` = 'DELIVERABLE_BRIDE_VOW' LIMIT 1;

INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'BOARD_SUMMARY_COMMON_PROMPT', 0, 'APPEND', NOW() FROM `prompt_scene` s WHERE s.`code` = 'BOARD_SUMMARY_COMMON' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'FAMILY_ORIGIN_SYSTEM', 0, 'PREPEND', NOW() FROM `prompt_scene` s WHERE s.`code` = 'BOARD_SUMMARY_FAMILY_ORIGIN' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'GROWTH_SYSTEM', 0, 'PREPEND', NOW() FROM `prompt_scene` s WHERE s.`code` = 'BOARD_SUMMARY_GROWTH' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'LOVE_STORY_SYSTEM', 0, 'PREPEND', NOW() FROM `prompt_scene` s WHERE s.`code` = 'BOARD_SUMMARY_LOVE_STORY' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'FUTURE_PROMISE_SYSTEM', 0, 'PREPEND', NOW() FROM `prompt_scene` s WHERE s.`code` = 'BOARD_SUMMARY_FUTURE_PROMISE' LIMIT 1;

INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'FAMILY_ORIGIN_SYSTEM', 0, 'PREPEND', NOW() FROM `prompt_scene` s WHERE s.`code` = 'BOARD_INTERVIEW_FAMILY_ORIGIN_GROOM' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'FAMILY_ORIGIN_SYSTEM', 0, 'PREPEND', NOW() FROM `prompt_scene` s WHERE s.`code` = 'BOARD_INTERVIEW_FAMILY_ORIGIN_BRIDE' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'GROWTH_SYSTEM', 0, 'PREPEND', NOW() FROM `prompt_scene` s WHERE s.`code` = 'BOARD_INTERVIEW_GROWTH_GROOM' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'GROWTH_SYSTEM', 0, 'PREPEND', NOW() FROM `prompt_scene` s WHERE s.`code` = 'BOARD_INTERVIEW_GROWTH_BRIDE' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'LOVE_STORY_SYSTEM', 0, 'PREPEND', NOW() FROM `prompt_scene` s WHERE s.`code` = 'BOARD_INTERVIEW_LOVE_STORY_GROOM' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'LOVE_STORY_SYSTEM', 0, 'PREPEND', NOW() FROM `prompt_scene` s WHERE s.`code` = 'BOARD_INTERVIEW_LOVE_STORY_BRIDE' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'FUTURE_PROMISE_SYSTEM', 0, 'PREPEND', NOW() FROM `prompt_scene` s WHERE s.`code` = 'BOARD_INTERVIEW_FUTURE_PROMISE_GROOM' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'FUTURE_PROMISE_SYSTEM', 0, 'PREPEND', NOW() FROM `prompt_scene` s WHERE s.`code` = 'BOARD_INTERVIEW_FUTURE_PROMISE_BRIDE' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'INTERVIEW_COMMON_SYSTEM', 0, 'PREPEND', NOW() FROM `prompt_scene` s WHERE s.`code` = 'BOARD_INTERVIEW_COMMON_GROOM' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'INTERVIEW_COMMON_SYSTEM', 0, 'PREPEND', NOW() FROM `prompt_scene` s WHERE s.`code` = 'BOARD_INTERVIEW_COMMON_BRIDE' LIMIT 1;
