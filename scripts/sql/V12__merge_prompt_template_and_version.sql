-- ============================================================
-- 提示词模板与提示词合并：prompt_template + prompt_version -> prompt
-- 场景项引用由 template_id 改为 prompt_code
-- ============================================================

-- 1. 新建 prompt 表
CREATE TABLE IF NOT EXISTS `prompt` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code` VARCHAR(64) NOT NULL COMMENT '逻辑编码，同 code 多行=多版本',
    `name` VARCHAR(100) NOT NULL COMMENT '显示名称',
    `content_type` VARCHAR(32) NOT NULL DEFAULT 'TEXT' COMMENT '类型',
    `description` VARCHAR(500) NULL COMMENT '说明',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `version_no` INT NOT NULL COMMENT '版本号',
    `content` TEXT NOT NULL COMMENT '提示词正文',
    `is_active` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否当前生效',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT NULL COMMENT '创建人',
    `updated_at` DATETIME NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code_version` (`code`, `version_no`),
    KEY `idx_code` (`code`),
    KEY `idx_is_active` (`code`, `is_active`),
    KEY `idx_status` (`status`),
    KEY `idx_content_type_status` (`content_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提示词表（模板+版本合并）';

-- 2. 从 template + version 迁移数据到 prompt
INSERT INTO `prompt` (`code`, `name`, `content_type`, `description`, `status`, `version_no`, `content`, `is_active`, `created_at`, `created_by`, `updated_at`)
SELECT t.`code`, t.`name`, t.`content_type`, t.`description`, t.`status`,
       v.`version_no`, v.`content`, IF(v.`is_active`, 1, 0), v.`created_at`, v.`created_by`, NOW()
FROM `prompt_template` t
JOIN `prompt_version` v ON v.`template_id` = t.`id`;

-- 3. 场景项：增加 prompt_code 列并回填
ALTER TABLE `prompt_scene_item` ADD COLUMN `prompt_code` VARCHAR(64) NULL COMMENT '提示词编码' AFTER `scene_id`;
UPDATE `prompt_scene_item` i
JOIN `prompt_template` t ON t.`id` = i.`template_id`
SET i.`prompt_code` = t.`code`;
ALTER TABLE `prompt_scene_item` MODIFY COLUMN `prompt_code` VARCHAR(64) NOT NULL;

-- 4. 删除场景项上的模板外键与唯一约束，再删 template_id
ALTER TABLE `prompt_scene_item` DROP FOREIGN KEY `fk_item_template`;
ALTER TABLE `prompt_scene_item` DROP KEY `uk_scene_template`;
ALTER TABLE `prompt_scene_item` DROP COLUMN `template_id`;
ALTER TABLE `prompt_scene_item` ADD UNIQUE KEY `uk_scene_prompt_code` (`scene_id`, `prompt_code`);
ALTER TABLE `prompt_scene_item` ADD KEY `idx_prompt_code` (`prompt_code`);

-- 5. 删除旧表（先删 version 再删 template，因 version 有 FK 指向 template）
DROP TABLE IF EXISTS `prompt_version`;
DROP TABLE IF EXISTS `prompt_template`;
