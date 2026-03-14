-- 用户表增加密码哈希字段，支持手机号+密码登录
ALTER TABLE `user` ADD COLUMN `password_hash` VARCHAR(128) NULL COMMENT '密码哈希(BCrypt)' AFTER `avatar_url`;
