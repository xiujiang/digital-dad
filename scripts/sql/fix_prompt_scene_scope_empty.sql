-- 修复 prompt_scene 中 scope 为空字符串的记录（导致 No enum constant PromptSceneScope）
-- 原因：数据库 scope 列为 ENUM，仅含 BOARD_INTERVIEW/BOARD_SUMMARY/DELIVERABLE，缺少 BOARD_STORY，
--       且两条「板块故事」场景的 scope 被存成空，需先扩展 ENUM 再更新。
-- 影响: id=30 BOARD_STORY_COMMON, id=31 BOARD_STORY_FAMILY_ORIGIN

-- 1. 扩展 scope 枚举，加入 BOARD_STORY（与 Java PromptSceneScope 一致）
ALTER TABLE `prompt_scene`
MODIFY COLUMN `scope` ENUM('BOARD_INTERVIEW','BOARD_SUMMARY','BOARD_STORY','DELIVERABLE') NOT NULL;

-- 2. 将空 scope 的「板块故事」场景改为 BOARD_STORY
UPDATE `prompt_scene`
SET `scope` = 'BOARD_STORY', `updated_at` = NOW()
WHERE `scope` = '' OR `scope` IS NULL;
