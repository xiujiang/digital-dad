-- ============================================================
-- 数字爸爸 v0.1 - 会话模块表结构
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- 依赖: V2__project_module.sql, V3__board_module.sql
-- ============================================================
-- 说明：
--   1. interview_session：采访会话
--   2. conversation_message：对话消息（含 batch_no、is_submitted 支持未提交可删改）
--   3. board_summary：板块小结（结构化提炼）
--   4. summary_item：小结条目
--   5. board_story：故事/时光（叙事内容）
--   6. key_person：关键人物
--   7. material_snapshot：素材快照（确认后冻结）
-- ============================================================

-- 1. 采访会话表
CREATE TABLE IF NOT EXISTS `interview_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `participant_id` BIGINT NOT NULL COMMENT '参与者ID',
    `current_project_board_id` BIGINT NULL COMMENT '当前板块(关联project_board)',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: READY/ACTIVE/WAITING_CONFIRM/COMPLETED/INTERRUPTED',
    `round_count` INT NOT NULL DEFAULT 0 COMMENT '对话轮数(用于x轮触发小结)',
    `started_at` DATETIME NULL COMMENT '开始时间',
    `ended_at` DATETIME NULL COMMENT '结束时间',
    `last_active_at` DATETIME NULL COMMENT '最后活跃时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_participant_id` (`participant_id`),
    KEY `idx_status` (`status`),
    KEY `idx_last_active` (`last_active_at`),
    CONSTRAINT `fk_session_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_session_participant` FOREIGN KEY (`participant_id`) REFERENCES `project_participant` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_session_board` FOREIGN KEY (`current_project_board_id`) REFERENCES `project_board` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采访会话表';

-- 2. 对话消息表
CREATE TABLE IF NOT EXISTS `conversation_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `session_id` BIGINT NOT NULL COMMENT '会话ID',
    `sender_type` VARCHAR(20) NOT NULL COMMENT '发送方: USER/AI/SYSTEM',
    `message_type` VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT '类型: TEXT/AUDIO',
    `content` TEXT NULL COMMENT '消息内容',
    `audio_url` VARCHAR(500) NULL COMMENT '语音url',
    `transcript_text` TEXT NULL COMMENT '转写文本',
    `sequence_no` INT NOT NULL DEFAULT 0 COMMENT '顺序号',
    `batch_no` INT NOT NULL DEFAULT 0 COMMENT '批次号(第几次确认提交)',
    `is_submitted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已提交给AI 0否1是,未提交可删改',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_session_sequence` (`session_id`, `sequence_no`),
    KEY `idx_session_batch` (`session_id`, `batch_no`),
    CONSTRAINT `fk_message_session` FOREIGN KEY (`session_id`) REFERENCES `interview_session` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话消息表';

-- 3. 板块小结表
CREATE TABLE IF NOT EXISTS `board_summary` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `session_id` BIGINT NOT NULL COMMENT '会话ID',
    `participant_id` BIGINT NOT NULL COMMENT '参与者ID',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `project_board_id` BIGINT NOT NULL COMMENT '板块ID(project_board)',
    `version_no` INT NOT NULL DEFAULT 1 COMMENT '版本号',
    `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/GENERATED/WAITING_CONFIRM/CONFIRMED',
    `title` VARCHAR(100) NULL COMMENT '标题',
    `content_json` TEXT NULL COMMENT '原始生成JSON',
    `generated_at` DATETIME NULL COMMENT '生成时间',
    `confirmed_at` DATETIME NULL COMMENT '确认时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_participant_board` (`participant_id`, `project_board_id`),
    CONSTRAINT `fk_summary_session` FOREIGN KEY (`session_id`) REFERENCES `interview_session` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_summary_participant` FOREIGN KEY (`participant_id`) REFERENCES `project_participant` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_summary_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_summary_board` FOREIGN KEY (`project_board_id`) REFERENCES `project_board` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='板块小结表';

-- 4. 小结条目表
CREATE TABLE IF NOT EXISTS `summary_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `summary_id` BIGINT NOT NULL COMMENT '小结ID',
    `item_type` VARCHAR(20) NOT NULL DEFAULT 'FACT' COMMENT '类型: FACT/EXPRESSION',
    `content` VARCHAR(500) NOT NULL COMMENT '条目内容',
    `item_order` INT NOT NULL DEFAULT 0 COMMENT '顺序',
    `is_selected` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否勾选进入素材 0否1是',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_summary_id` (`summary_id`),
    CONSTRAINT `fk_item_summary` FOREIGN KEY (`summary_id`) REFERENCES `board_summary` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小结条目表';

-- 5. 故事/时光表
CREATE TABLE IF NOT EXISTS `board_story` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `session_id` BIGINT NOT NULL COMMENT '会话ID',
    `participant_id` BIGINT NOT NULL COMMENT '参与者ID',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `project_board_id` BIGINT NOT NULL COMMENT '板块ID',
    `content` TEXT NOT NULL COMMENT '叙事内容',
    `version_no` INT NOT NULL DEFAULT 1 COMMENT '版本号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_participant_board` (`participant_id`, `project_board_id`),
    CONSTRAINT `fk_story_session` FOREIGN KEY (`session_id`) REFERENCES `interview_session` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_story_participant` FOREIGN KEY (`participant_id`) REFERENCES `project_participant` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_story_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_story_board` FOREIGN KEY (`project_board_id`) REFERENCES `project_board` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='故事/时光表';

-- 6. 关键人物表
CREATE TABLE IF NOT EXISTS `key_person` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `session_id` BIGINT NOT NULL COMMENT '会话ID',
    `participant_id` BIGINT NOT NULL COMMENT '参与者ID',
    `name` VARCHAR(50) NOT NULL COMMENT '人物称谓',
    `role_label` VARCHAR(50) NULL COMMENT '身份标签,如父亲/高中同学',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_participant_id` (`participant_id`),
    CONSTRAINT `fk_person_session` FOREIGN KEY (`session_id`) REFERENCES `interview_session` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_person_participant` FOREIGN KEY (`participant_id`) REFERENCES `project_participant` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关键人物表';

-- 7. 素材快照表(确认后冻结,供交付物生成)
CREATE TABLE IF NOT EXISTS `material_snapshot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `participant_id` BIGINT NOT NULL COMMENT '参与者ID',
    `project_board_id` BIGINT NOT NULL COMMENT '板块ID',
    `summary_id` BIGINT NOT NULL COMMENT '对应board_summary',
    `snapshot_payload` TEXT NOT NULL COMMENT '已确认条目快照(JSON)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_participant_id` (`participant_id`),
    KEY `idx_project_participant` (`project_id`, `participant_id`),
    CONSTRAINT `fk_snapshot_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_snapshot_participant` FOREIGN KEY (`participant_id`) REFERENCES `project_participant` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_snapshot_board` FOREIGN KEY (`project_board_id`) REFERENCES `project_board` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_snapshot_summary` FOREIGN KEY (`summary_id`) REFERENCES `board_summary` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='素材快照表';
