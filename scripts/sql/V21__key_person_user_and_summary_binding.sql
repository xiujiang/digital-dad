-- 关键人物改为用户维度 + 小结绑定角色
-- 1. key_person 增加 user_id，存量从 participant 回填
ALTER TABLE `key_person`
    ADD COLUMN `user_id` BIGINT NULL COMMENT '用户ID(角色库归属)' AFTER `participant_id`,
    ADD KEY `idx_user_id` (`user_id`);

UPDATE `key_person` kp
JOIN `project_participant` pp ON kp.participant_id = pp.id
SET kp.user_id = pp.user_id;

ALTER TABLE `key_person`
    MODIFY COLUMN `user_id` BIGINT NOT NULL COMMENT '用户ID(角色库归属)',
    MODIFY COLUMN `session_id` BIGINT NULL COMMENT '创建时会话ID(可选)',
    MODIFY COLUMN `participant_id` BIGINT NULL COMMENT '创建时参与者ID(可选)',
    ADD CONSTRAINT `fk_key_person_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE;

-- 2. 小结与关键人物多对多关联表
CREATE TABLE IF NOT EXISTS `board_summary_key_person` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `summary_id` BIGINT NOT NULL COMMENT '小结ID',
    `key_person_id` BIGINT NOT NULL COMMENT '关键人物ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_summary_person` (`summary_id`, `key_person_id`),
    KEY `idx_summary_id` (`summary_id`),
    KEY `idx_key_person_id` (`key_person_id`),
    CONSTRAINT `fk_bskp_summary` FOREIGN KEY (`summary_id`) REFERENCES `board_summary` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_bskp_key_person` FOREIGN KEY (`key_person_id`) REFERENCES `key_person` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小结绑定关键人物';
