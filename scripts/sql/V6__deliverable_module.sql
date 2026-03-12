-- ============================================================
-- 数字爸爸 v0.1 - 交付物模块
-- 依赖: V2__project_module, V5__session_module
-- 说明: generated_content 表已在 V2 中定义，本脚本补充交付物相关字段
-- ============================================================

-- 为 generated_content 增加快照版本追踪，用于检测「待更新」状态
-- 生成时记录所用素材快照的最大 created_at，若之后有快照更新则标记 OUTDATED
-- snapshot_version_at: 生成时记录所用素材快照的最大 created_at，用于 OUTDATED 检测
ALTER TABLE `generated_content`
    ADD COLUMN `snapshot_version_at` DATETIME NULL
    COMMENT '生成时素材快照版本时间(用于检测待更新)' AFTER `updated_at`;
