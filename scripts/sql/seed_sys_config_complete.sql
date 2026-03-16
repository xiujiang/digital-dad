-- ============================================================
-- 补全 sys_config 表数据（可重复执行）
-- 确保 member.packages、speech.transcription、interview、security.password_policy 均存在且结构完整
-- 依赖: 表 sys_config 已存在（如 V7__sys_config.sql）
-- ============================================================

-- 1. 会员套餐配置
INSERT INTO `sys_config` (`config_key`, `config_value`, `description`, `created_at`, `updated_at`)
VALUES (
    'member.packages',
    JSON_OBJECT(
        'name', '会员套餐配置',
        'annual', JSON_OBJECT(
            'name', '年费会员',
            'quota', 60,
            'valid_days', 365
        ),
        'single', JSON_OBJECT(
            'name', '单次会员',
            'quota', 1,
            'valid_days', NULL
        )
    ),
    '会员套餐配置：annual 年费会员、single 单次会员',
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE
    `config_value` = VALUES(`config_value`),
    `description` = VALUES(`description`),
    `updated_at` = NOW();

-- 2. 语音转写配置（默认配额秒数 + 单条语音最大时长）
INSERT INTO `sys_config` (`config_key`, `config_value`, `description`, `created_at`, `updated_at`)
VALUES (
    'speech.transcription',
    JSON_OBJECT(
        'name', '语音转写配置',
        'default_seconds', 3600,
        'max_voice_seconds', 180
    ),
    '语音转写：默认配额(秒)、单条语音最大时长(秒)，首次使用时懒加载初始化',
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE
    `config_value` = VALUES(`config_value`),
    `description` = VALUES(`description`),
    `updated_at` = NOW();

-- 3. 采访配置（板块聊天轮数上限）
INSERT INTO `sys_config` (`config_key`, `config_value`, `description`, `created_at`, `updated_at`)
VALUES (
    'interview',
    JSON_OBJECT(
        'name', '采访配置',
        'max_rounds_per_board', 10
    ),
    '采访配置：板块聊天轮数上限',
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE
    `config_value` = VALUES(`config_value`),
    `description` = VALUES(`description`),
    `updated_at` = NOW();

-- 4. 密码策略配置
INSERT INTO `sys_config` (`config_key`, `config_value`, `description`, `created_at`, `updated_at`)
VALUES (
    'security.password_policy',
    JSON_OBJECT(
        'name', '密码策略',
        'enforceStrongPassword', true,
        'requirePasswordChangePeriodically', false,
        'passwordChangeIntervalDays', 90
    ),
    '密码策略：强制强密码、定期修改密码',
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE
    `config_value` = VALUES(`config_value`),
    `description` = VALUES(`description`),
    `updated_at` = NOW();
