-- 密码策略配置 + User 表 last_password_changed_at
-- 1. 初始化密码策略配置
INSERT INTO `sys_config` (`config_key`, `config_value`, `description`)
VALUES (
    'security.password_policy',
    JSON_OBJECT(
        'enforceStrongPassword', true,
        'requirePasswordChangePeriodically', false,
        'passwordChangeIntervalDays', 90
    ),
    '密码策略：强制强密码、定期修改密码'
) ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

-- 2. 用户表增加最后修改密码时间
ALTER TABLE `user` ADD COLUMN `last_password_changed_at` DATETIME NULL COMMENT '最后修改密码时间' AFTER `password_hash`;
