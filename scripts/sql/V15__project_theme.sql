-- ============================================================
-- 项目表增加主题字段
-- 依赖: V2__project_module.sql
-- ============================================================

ALTER TABLE `project`
    ADD COLUMN `theme` VARCHAR(200) NULL
    COMMENT '婚礼主题（如：婚礼故事采访、浪漫秋日婚礼等）' AFTER `wedding_date`;
