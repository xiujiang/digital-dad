-- ============================================================
-- V16 乱码修复：根据 V16 种子数据修正 name/description（UTF-8）
-- 执行前请确保 run_sql_remote.py 已加 --default-character-set=utf8mb4
-- prompt 的 content 若仍乱码，请重新执行 V16__seed_prompt_and_scene_full_content.sql
-- ============================================================

-- prompt 表：按 code 更新 name、description
UPDATE `prompt` SET `name` = '婚礼开场白-主文案', `description` = '婚礼开场白主提示词（四乐章结构）', `updated_at` = NOW() WHERE `code` = 'OPENING_SPEECH_MAIN';
UPDATE `prompt` SET `name` = '新郎誓言-主文案', `description` = '新郎誓言主提示词', `updated_at` = NOW() WHERE `code` = 'GROOM_VOW_MAIN';
UPDATE `prompt` SET `name` = '新娘誓言-主文案', `description` = '新娘誓言主提示词', `updated_at` = NOW() WHERE `code` = 'BRIDE_VOW_MAIN';
UPDATE `prompt` SET `name` = '对话小结-结构化提炼', `description` = '对话小结JSON输出规范', `updated_at` = NOW() WHERE `code` = 'BOARD_SUMMARY_COMMON_PROMPT';
UPDATE `prompt` SET `name` = '原生家庭-系统角色', `description` = '采访原生家庭时的系统角色与访谈流', `updated_at` = NOW() WHERE `code` = 'FAMILY_ORIGIN_SYSTEM';
UPDATE `prompt` SET `name` = '成长轨迹-系统角色', `description` = '采访成长轨迹时的系统角色与访谈流', `updated_at` = NOW() WHERE `code` = 'GROWTH_SYSTEM';
UPDATE `prompt` SET `name` = '爱情故事-系统角色', `description` = '采访爱情故事时的系统角色与访谈流', `updated_at` = NOW() WHERE `code` = 'LOVE_STORY_SYSTEM';
UPDATE `prompt` SET `name` = '未来承诺-系统角色', `description` = '采访未来承诺时的系统角色与访谈流', `updated_at` = NOW() WHERE `code` = 'FUTURE_PROMISE_SYSTEM';

-- prompt_scene 表：按 code 更新 name、description
UPDATE `prompt_scene` SET `name` = '未来承诺·板块小结', `description` = '未来承诺板块小结', `updated_at` = NOW() WHERE `code` = 'BOARD_SUMMARY_FUTURE_PROMISE';
UPDATE `prompt_scene` SET `name` = '采访-成长轨迹-新娘', `description` = '新娘侧成长轨迹采访', `updated_at` = NOW() WHERE `code` = 'BOARD_INTERVIEW_GROWTH_BRIDE';
UPDATE `prompt_scene` SET `name` = '采访-爱情故事-新娘', `description` = '新娘侧爱情故事采访', `updated_at` = NOW() WHERE `code` = 'BOARD_INTERVIEW_LOVE_STORY_BRIDE';
UPDATE `prompt_scene` SET `name` = '采访-未来承诺-新郎', `description` = '新郎侧未来承诺采访', `updated_at` = NOW() WHERE `code` = 'BOARD_INTERVIEW_FUTURE_PROMISE_GROOM';
UPDATE `prompt_scene` SET `name` = '采访-未来承诺-新娘', `description` = '新娘侧未来承诺采访', `updated_at` = NOW() WHERE `code` = 'BOARD_INTERVIEW_FUTURE_PROMISE_BRIDE';
