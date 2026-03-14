-- ============================================================
-- 场景/提示词测试数据（可重复执行：重复的 code/uk 会忽略）
-- 依赖: V12__merge_prompt_template_and_version.sql (prompt, prompt_scene, prompt_scene_item)
-- ============================================================

-- 1. 提示词：每条 (code, version_no=1) 一行，is_active=1
INSERT IGNORE INTO `prompt` (`code`, `name`, `content_type`, `description`, `status`, `version_no`, `content`, `is_active`, `created_at`, `updated_at`)
VALUES
('OPENING_SPEECH_MAIN', '婚礼开场白-主文案', 'TEXT', '婚礼开场白主提示词', 'ENABLED', 1,
 '你是一位温暖、专业的主持人。请根据以下婚礼信息，撰写一段 2～3 分钟的开场白，要点：欢迎来宾、点明主题、自然过渡到仪式。语气庄重而不失亲切。', 1, NOW(), NOW()),
('GROOM_VOW_MAIN', '新郎誓言-主文案', 'TEXT', '新郎誓言主提示词', 'ENABLED', 1,
 '请以新郎的口吻撰写婚礼誓言，约 1～2 分钟。内容可包含：对伴侣的感谢、相识相知的回忆、承诺与期许。真诚、简洁、可带一点幽默。', 1, NOW(), NOW()),
('BRIDE_VOW_MAIN', '新娘誓言-主文案', 'TEXT', '新娘誓言主提示词', 'ENABLED', 1,
 '请以新娘的口吻撰写婚礼誓言，约 1～2 分钟。内容可包含：对伴侣的感谢、两人之间的重要时刻、承诺与愿望。语气温柔、真挚。', 1, NOW(), NOW()),
('FAMILY_ORIGIN_SYSTEM', '原生家庭-系统角色', 'SYSTEM_ROLE', '采访原生家庭时的系统角色', 'ENABLED', 1,
 '你是一位善于倾听的婚礼采访助手。当前板块为「原生家庭」，请用温和、开放的方式引导对方回忆与家人相关的故事，避免审问感。', 1, NOW(), NOW()),
('FAMILY_ORIGIN_QUESTIONS', '原生家庭-提问列表', 'QUESTION', '原生家庭板块的提问', 'ENABLED', 1,
 '请针对「原生家庭」主题生成 5～8 个开放式问题，例如：成长中谁对你影响最大、家里有什么传统、父母如何相处等。问题要具体、易于回答。', 1, NOW(), NOW()),
('GROWTH_SYSTEM', '成长轨迹-系统角色', 'SYSTEM_ROLE', '采访成长轨迹时的系统角色', 'ENABLED', 1,
 '你是一位婚礼采访助手。当前板块为「成长轨迹」，请引导对方分享求学、工作或人生转折中的故事，与婚礼主题自然衔接。', 1, NOW(), NOW()),
('GROWTH_QUESTIONS', '成长轨迹-提问列表', 'QUESTION', '成长轨迹板块的提问', 'ENABLED', 1,
 '请针对「成长轨迹」主题生成 5～8 个开放式问题，如：求学阶段最难忘的事、第一份工作的感受、人生中的贵人等。', 1, NOW(), NOW()),
('LOVE_STORY_SYSTEM', '爱情故事-系统角色', 'SYSTEM_ROLE', '采访爱情故事时的系统角色', 'ENABLED', 1,
 '你是一位婚礼采访助手。当前板块为「爱情故事」，请用轻松、浪漫的基调引导对方讲述两人相识、相恋的重要时刻。', 1, NOW(), NOW()),
('LOVE_STORY_QUESTIONS', '爱情故事-提问列表', 'QUESTION', '爱情故事板块的提问', 'ENABLED', 1,
 '请针对「爱情故事」主题生成 5～8 个开放式问题，如：第一次见面、第一次约会、求婚经历、最感动的小事等。', 1, NOW(), NOW()),
('BOARD_SUMMARY_COMMON_PROMPT', '通用板块小结', 'SUMMARY', '通用板块小结生成', 'ENABLED', 1,
 '请根据以上采访内容，整理成一段 200～400 字的板块小结，突出关键故事与情感，语言流畅、适合在婚礼故事中展示。', 1, NOW(), NOW());

-- 2. 场景：交付物 + 板块小结 + 采访
INSERT IGNORE INTO `prompt_scene` (`code`, `name`, `scope`, `board_code`, `role_type`, `description`, `status`, `created_at`, `updated_at`)
VALUES
('DELIVERABLE_OPENING_SPEECH', '婚礼开场白', 'DELIVERABLE', NULL, NULL, '生成婚礼开场白文案', 'ENABLED', NOW(), NOW()),
('DELIVERABLE_GROOM_VOW', '新郎誓言', 'DELIVERABLE', NULL, NULL, '生成新郎誓言', 'ENABLED', NOW(), NOW()),
('DELIVERABLE_BRIDE_VOW', '新娘誓言', 'DELIVERABLE', NULL, NULL, '生成新娘誓言', 'ENABLED', NOW(), NOW()),
('BOARD_SUMMARY_COMMON', '通用板块小结', 'BOARD_SUMMARY', NULL, NULL, '无板块时的兜底小结场景', 'ENABLED', NOW(), NOW()),
('BOARD_SUMMARY_FAMILY_ORIGIN', '原生家庭·板块小结', 'BOARD_SUMMARY', 'FAMILY_ORIGIN', NULL, '原生家庭板块小结', 'ENABLED', NOW(), NOW()),
('BOARD_SUMMARY_GROWTH', '成长轨迹·板块小结', 'BOARD_SUMMARY', 'GROWTH', NULL, '成长轨迹板块小结', 'ENABLED', NOW(), NOW()),
('BOARD_SUMMARY_LOVE_STORY', '爱情故事·板块小结', 'BOARD_SUMMARY', 'LOVE_STORY', NULL, '爱情故事板块小结', 'ENABLED', NOW(), NOW()),
('BOARD_INTERVIEW_FAMILY_ORIGIN_GROOM', '采访-原生家庭-新郎', 'BOARD_INTERVIEW', 'FAMILY_ORIGIN', 'GROOM', '新郎侧原生家庭采访', 'ENABLED', NOW(), NOW()),
('BOARD_INTERVIEW_FAMILY_ORIGIN_BRIDE', '采访-原生家庭-新娘', 'BOARD_INTERVIEW', 'FAMILY_ORIGIN', 'BRIDE', '新娘侧原生家庭采访', 'ENABLED', NOW(), NOW()),
('BOARD_INTERVIEW_GROWTH_GROOM', '采访-成长轨迹-新郎', 'BOARD_INTERVIEW', 'GROWTH', 'GROOM', '新郎侧成长轨迹采访', 'ENABLED', NOW(), NOW()),
('BOARD_INTERVIEW_LOVE_STORY_GROOM', '采访-爱情故事-新郎', 'BOARD_INTERVIEW', 'LOVE_STORY', 'GROOM', '新郎侧爱情故事采访', 'ENABLED', NOW(), NOW());

-- 3. 场景项：绑定提示词到场景（通过 scene code 查 scene_id）
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'OPENING_SPEECH_MAIN', 0, 'APPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'DELIVERABLE_OPENING_SPEECH' LIMIT 1;

INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'GROOM_VOW_MAIN', 0, 'APPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'DELIVERABLE_GROOM_VOW' LIMIT 1;

INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'BRIDE_VOW_MAIN', 0, 'APPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'DELIVERABLE_BRIDE_VOW' LIMIT 1;

INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'BOARD_SUMMARY_COMMON_PROMPT', 0, 'APPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'BOARD_SUMMARY_COMMON' LIMIT 1;

INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'FAMILY_ORIGIN_SYSTEM', 0, 'PREPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'BOARD_SUMMARY_FAMILY_ORIGIN' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'FAMILY_ORIGIN_QUESTIONS', 1, 'APPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'BOARD_SUMMARY_FAMILY_ORIGIN' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'BOARD_SUMMARY_COMMON_PROMPT', 2, 'APPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'BOARD_SUMMARY_FAMILY_ORIGIN' LIMIT 1;

INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'GROWTH_SYSTEM', 0, 'PREPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'BOARD_SUMMARY_GROWTH' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'GROWTH_QUESTIONS', 1, 'APPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'BOARD_SUMMARY_GROWTH' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'BOARD_SUMMARY_COMMON_PROMPT', 2, 'APPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'BOARD_SUMMARY_GROWTH' LIMIT 1;

INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'LOVE_STORY_SYSTEM', 0, 'PREPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'BOARD_SUMMARY_LOVE_STORY' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'LOVE_STORY_QUESTIONS', 1, 'APPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'BOARD_SUMMARY_LOVE_STORY' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'BOARD_SUMMARY_COMMON_PROMPT', 2, 'APPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'BOARD_SUMMARY_LOVE_STORY' LIMIT 1;

-- 采访场景：系统角色 + 提问
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'FAMILY_ORIGIN_SYSTEM', 0, 'PREPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'BOARD_INTERVIEW_FAMILY_ORIGIN_GROOM' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'FAMILY_ORIGIN_QUESTIONS', 1, 'APPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'BOARD_INTERVIEW_FAMILY_ORIGIN_GROOM' LIMIT 1;

INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'FAMILY_ORIGIN_SYSTEM', 0, 'PREPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'BOARD_INTERVIEW_FAMILY_ORIGIN_BRIDE' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'FAMILY_ORIGIN_QUESTIONS', 1, 'APPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'BOARD_INTERVIEW_FAMILY_ORIGIN_BRIDE' LIMIT 1;

INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'GROWTH_SYSTEM', 0, 'PREPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'BOARD_INTERVIEW_GROWTH_GROOM' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'GROWTH_QUESTIONS', 1, 'APPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'BOARD_INTERVIEW_GROWTH_GROOM' LIMIT 1;

INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'LOVE_STORY_SYSTEM', 0, 'PREPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'BOARD_INTERVIEW_LOVE_STORY_GROOM' LIMIT 1;
INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'LOVE_STORY_QUESTIONS', 1, 'APPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'BOARD_INTERVIEW_LOVE_STORY_GROOM' LIMIT 1;
