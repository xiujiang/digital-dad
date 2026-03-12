-- ============================================================
-- 数字爸爸 v0.1 - 项目模块表结构
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- 依赖: V1__user_module.sql (user 表)
-- ============================================================

-- 1. 项目主表
CREATE TABLE IF NOT EXISTS `project` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `project_no` VARCHAR(32) NOT NULL COMMENT '项目编号',
    `host_user_id` BIGINT NOT NULL COMMENT '主持人user_id',
    `groom_name` VARCHAR(50) NULL COMMENT '新郎姓名(创建时填写)',
    `bride_name` VARCHAR(50) NULL COMMENT '新娘姓名(创建时填写)',
    `wedding_date` DATE NULL COMMENT '婚礼日期',
    `share_token` VARCHAR(64) NOT NULL COMMENT '分享令牌(用于生成链接/二维码)',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: DRAFT/ACTIVE/COMPLETED/ARCHIVED/DISABLED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME NULL COMMENT '逻辑删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_project_no` (`project_no`),
    UNIQUE KEY `uk_share_token` (`share_token`),
    KEY `idx_host_user_id` (`host_user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_deleted_at` (`deleted_at`),
    CONSTRAINT `fk_project_host` FOREIGN KEY (`host_user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目主表';

-- 2. 项目参与者表(用户扫码选择身份后绑定)
CREATE TABLE IF NOT EXISTS `project_participant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID(关联user表)',
    `role_type` VARCHAR(20) NOT NULL COMMENT '角色: GROOM/BRIDE',
    `status` VARCHAR(20) NOT NULL DEFAULT 'INVITED' COMMENT '状态: INVITED/ENTERED/IN_PROGRESS/COMPLETED/ABANDONED',
    `current_board_order` INT NULL COMMENT '当前进行到第几板块(1-4)',
    `joined_at` DATETIME NULL COMMENT '绑定/进入时间',
    `last_active_at` DATETIME NULL COMMENT '最后活跃时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_project_role` (`project_id`, `role_type`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_participant_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_participant_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目参与者表';

-- 3. 生成物表(与项目绑定，交付物模块创建，项目模块按项目查询)
CREATE TABLE IF NOT EXISTS `generated_content` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `participant_id` BIGINT NULL COMMENT '参与者ID(新郎誓言/新娘誓言有值，开场白为空)',
    `content_type` VARCHAR(20) NOT NULL COMMENT '类型: OPENING_SPEECH/GROOM_VOW/BRIDE_VOW',
    `version_no` INT NOT NULL DEFAULT 1 COMMENT '版本号',
    `title` VARCHAR(100) NULL COMMENT '标题',
    `content` TEXT NULL COMMENT '内容',
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/ACTIVE/OUTDATED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_participant_id` (`participant_id`),
    KEY `idx_content_type` (`content_type`),
    KEY `idx_project_type` (`project_id`, `content_type`),
    CONSTRAINT `fk_content_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_content_participant` FOREIGN KEY (`participant_id`) REFERENCES `project_participant` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生成物表';
