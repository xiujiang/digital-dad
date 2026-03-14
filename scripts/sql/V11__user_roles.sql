-- ============================================================
-- 用户角色表：统一用户表 + 角色独立存储，支持一人多角色
-- ============================================================

-- 1. 用户角色表
CREATE TABLE IF NOT EXISTS `user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role` VARCHAR(32) NOT NULL COMMENT '角色: SUPER_ADMIN/HOST/WECHAT_USER 等',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role` (`role`),
    CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色表';

-- 2. 将现有 user.user_type 迁移到 user_role
INSERT INTO `user_role` (`user_id`, `role`)
SELECT `id`, `user_type` FROM `user` WHERE `deleted_at` IS NULL
ON DUPLICATE KEY UPDATE `user_id` = VALUES(`user_id`);

-- 3. 删除 user 表的 user_type 列（索引随列删除）
ALTER TABLE `user` DROP COLUMN `user_type`;
