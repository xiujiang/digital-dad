-- 为「成长轨迹」「爱情故事」「未来承诺」三个板块的小结场景补充 BOARD_SUMMARY_COMMON_PROMPT
-- 原因：getSummaryPrompts() 只保留 prompt_code=BOARD_SUMMARY_COMMON_PROMPT，只有原生家庭场景下挂了该提示词，其他三块过滤后为空导致小结用默认文案
-- 依赖：prompt_scene 表已有 BOARD_SUMMARY_GROWTH / BOARD_SUMMARY_LOVE_STORY / BOARD_SUMMARY_FUTURE_PROMISE；prompt 表已有 BOARD_SUMMARY_COMMON_PROMPT
-- 可重复执行：INSERT IGNORE + uk_scene_prompt_code 避免重复插入

INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'BOARD_SUMMARY_COMMON_PROMPT', 1, 'APPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'BOARD_SUMMARY_GROWTH' LIMIT 1;

INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'BOARD_SUMMARY_COMMON_PROMPT', 1, 'APPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'BOARD_SUMMARY_LOVE_STORY' LIMIT 1;

INSERT IGNORE INTO `prompt_scene_item` (`scene_id`, `prompt_code`, `display_order`, `usage_mode`, `created_at`)
SELECT s.`id`, 'BOARD_SUMMARY_COMMON_PROMPT', 1, 'APPEND', NOW()
FROM `prompt_scene` s WHERE s.`code` = 'BOARD_SUMMARY_FUTURE_PROMISE' LIMIT 1;
