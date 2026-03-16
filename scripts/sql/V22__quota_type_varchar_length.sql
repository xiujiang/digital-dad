-- ============================================================
-- 扩大 quota_type 列长度以容纳 SPEECH_TRANSCRIPTION 等枚举值
-- 原因：QuotaType.SPEECH_TRANSCRIPTION 为 21 字符，原 VARCHAR(20) 导致 Data truncated
-- ============================================================

ALTER TABLE `user_quota`
    MODIFY COLUMN `quota_type` VARCHAR(32) NOT NULL COMMENT '配额类型: PROJECT/GENERATION/SPEECH_TRANSCRIPTION';

ALTER TABLE `quota_flow`
    MODIFY COLUMN `quota_type` VARCHAR(32) NOT NULL COMMENT '配额类型: PROJECT/GENERATION/SPEECH_TRANSCRIPTION';
