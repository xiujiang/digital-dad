-- ============================================================
-- 数字爸爸 v0.1 - 用户模块表结构
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- ============================================================

-- 1. 用户主表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_type` VARCHAR(20) NOT NULL COMMENT '用户类型: SUPER_ADMIN/HOST/WECHAT_USER',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED/DISABLED',
    `phone` VARCHAR(11) NULL COMMENT '手机号(主持人必填)',
    `name` VARCHAR(50) NULL COMMENT '姓名/昵称',
    `avatar_url` VARCHAR(500) NULL COMMENT '头像URL',
    `contact_visible` VARCHAR(20) NULL DEFAULT 'PUBLIC' COMMENT '联系方式展示: PUBLIC/MASKED(仅主持人)',
    `last_login_at` DATETIME NULL COMMENT '最近登录时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME NULL COMMENT '逻辑删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_user_type` (`user_type`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户主表';

-- 2. 微信用户扩展表
CREATE TABLE IF NOT EXISTS `user_wechat` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '关联user.id',
    `openid` VARCHAR(64) NOT NULL COMMENT '微信openid',
    `unionid` VARCHAR(64) NULL COMMENT '微信unionid',
    `app_type` VARCHAR(20) NOT NULL COMMENT '来源: H5/MINI_PROGRAM/APP',
    `nickname` VARCHAR(100) NULL COMMENT '微信昵称',
    `avatar_url` VARCHAR(500) NULL COMMENT '微信头像',
    `gender` TINYINT NULL COMMENT '性别: 0未知/1男/2女',
    `country` VARCHAR(50) NULL COMMENT '国家',
    `province` VARCHAR(50) NULL COMMENT '省份',
    `city` VARCHAR(50) NULL COMMENT '城市',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    UNIQUE KEY `uk_app_openid` (`app_type`, `openid`),
    KEY `idx_unionid` (`unionid`),
    CONSTRAINT `fk_wechat_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微信用户扩展表';

-- 3. 超管扩展表(账号密码登录)
CREATE TABLE IF NOT EXISTS `user_admin` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '关联user.id',
    `account` VARCHAR(50) NOT NULL COMMENT '登录账号',
    `password_hash` VARCHAR(128) NOT NULL COMMENT '密码哈希',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    UNIQUE KEY `uk_account` (`account`),
    CONSTRAINT `fk_admin_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='超管扩展表';

-- 4. 会员信息表
CREATE TABLE IF NOT EXISTS `user_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '关联user.id',
    `member_type` VARCHAR(20) NOT NULL COMMENT '会员类型: HOST/WECHAT',
    `package_name` VARCHAR(50) NULL COMMENT '套餐名称',
    `package_quota` INT NULL COMMENT '套餐含总场次',
    `valid_from` DATETIME NULL COMMENT '有效期开始',
    `valid_to` DATETIME NULL COMMENT '有效期结束',
    `status` VARCHAR(20) NOT NULL COMMENT '状态: ACTIVE/EXPIRED/NOT_ACTIVATED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_member_type` (`user_id`, `member_type`),
    KEY `idx_status` (`status`),
    KEY `idx_valid_to` (`valid_to`),
    CONSTRAINT `fk_member_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员信息表';

-- 5. 配额表
CREATE TABLE IF NOT EXISTS `user_quota` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '关联user.id',
    `quota_type` VARCHAR(20) NOT NULL COMMENT '配额类型: PROJECT/GENERATION',
    `remaining` INT NOT NULL DEFAULT 0 COMMENT '剩余次数',
    `total_used` INT NOT NULL DEFAULT 0 COMMENT '累计使用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_quota_type` (`user_id`, `quota_type`),
    CONSTRAINT `fk_quota_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配额表';

-- 6. 配额流水表
CREATE TABLE IF NOT EXISTS `quota_flow` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `quota_type` VARCHAR(20) NOT NULL COMMENT '配额类型: PROJECT/GENERATION',
    `flow_type` VARCHAR(20) NOT NULL COMMENT '流水类型: RECHARGE/GIFT/DEDUCT/ROLLBACK',
    `delta` INT NOT NULL COMMENT '变动量(正负)',
    `balance_after` INT NULL COMMENT '变动后余额',
    `reason` VARCHAR(200) NULL COMMENT '变动原因',
    `ref_type` VARCHAR(50) NULL COMMENT '关联业务类型: PROJECT/ADMIN_ADJUST',
    `ref_id` VARCHAR(64) NULL COMMENT '关联业务ID',
    `operator_id` BIGINT NULL COMMENT '操作人user_id',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_user_created` (`user_id`, `created_at`),
    CONSTRAINT `fk_flow_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配额流水表';

-- 7. 验证码记录表(可选,用于审计;也可仅用Redis)
CREATE TABLE IF NOT EXISTS `sms_code` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `phone` VARCHAR(11) NOT NULL COMMENT '手机号',
    `code` VARCHAR(6) NOT NULL COMMENT '验证码',
    `scene` VARCHAR(20) NOT NULL COMMENT '场景: LOGIN/BIND',
    `expires_at` DATETIME NOT NULL COMMENT '过期时间',
    `used_at` DATETIME NULL COMMENT '使用时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_phone_scene` (`phone`, `scene`),
    KEY `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='验证码记录表';
