-- ============================================================
-- 语音转写：单条语音最长时长配置
-- 依赖: V8__speech_transcription_quota.sql
-- ============================================================

-- 在 speech.transcription 配置中增加 max_voice_seconds（默认 180 秒 = 3 分钟）
UPDATE `sys_config`
SET `config_value` = JSON_SET(COALESCE(`config_value`, JSON_OBJECT()), '$.max_voice_seconds', 180),
    `updated_at` = CURRENT_TIMESTAMP
WHERE `config_key` = 'speech.transcription';
