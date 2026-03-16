package com.digitaldad.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.dto.MemberPackageConfigDto;
import com.digitaldad.config.dto.PasswordPolicyConfigDto;
import com.digitaldad.entity.SysConfig;
import com.digitaldad.repository.SysConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 配置服务
 * <p>从 sys_config 表读取配置，提供会员套餐、语音转写默认配额等配置查询。</p>
 */
@Service
@RequiredArgsConstructor
public class ConfigService {

    public static final String KEY_MEMBER_PACKAGES = "member.packages";
    public static final String KEY_SPEECH_TRANSCRIPTION = "speech.transcription";
    public static final String KEY_INTERVIEW = "interview";
    public static final String KEY_PASSWORD_POLICY = "security.password_policy";

    private final SysConfigRepository sysConfigRepository;

    /**
     * 查询全部配置项（sys_config 表）
     */
    public List<SysConfig> listAllConfigs() {
        return sysConfigRepository.findAll();
    }

    /**
     * 按 configKey 修改配置值（仅更新已存在的 key，不存在则抛异常）
     *
     * @param configKey   配置键
     * @param configValue 新的配置值（JSON 对象，可含 name 等字段）
     */
    @Transactional
    public SysConfig updateConfig(String configKey, Map<String, Object> configValue) {
        if (configValue == null || configValue.isEmpty()) {
            throw new BusinessException(400, "配置值不能为空");
        }
        SysConfig config = sysConfigRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new BusinessException(404, "配置不存在: " + configKey));
        config.setConfigValue(configValue);
        return sysConfigRepository.save(config);
    }

    /**
     * 根据 key 获取配置（JSON 转为 Map）
     */
    public Map<String, Object> getConfigAsMap(String configKey) {
        SysConfig config = sysConfigRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new BusinessException(500, "配置不存在: " + configKey));
        Map<String, Object> value = config.getConfigValue();
        if (value == null) {
            throw new BusinessException(500, "配置值为空: " + configKey);
        }
        return value;
    }

    /**
     * 获取会员套餐配置
     *
     * @param packageCode 套餐编码，如 annual、single
     */
    @SuppressWarnings("unchecked")
    public MemberPackageConfigDto getMemberPackageConfig(String packageCode) {
        Map<String, Object> packages = getConfigAsMap(KEY_MEMBER_PACKAGES);
        Object pkg = packages.get(packageCode);
        if (pkg == null || !(pkg instanceof Map)) {
            throw new BusinessException(400, "未知的套餐类型: " + packageCode);
        }

        Map<String, Object> pkgMap = (Map<String, Object>) pkg;
        MemberPackageConfigDto dto = new MemberPackageConfigDto();
        dto.setName(getString(pkgMap, "name"));
        dto.setQuota(getInt(pkgMap, "quota"));
        dto.setValidDays(getIntOrNull(pkgMap, "valid_days"));
        return dto;
    }

    private String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private int getInt(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) {
            throw new BusinessException(500, "配置缺失: " + key);
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException(500, "配置格式错误: " + key + " 应为整数");
        }
    }

    /**
     * 获取语音转写默认配额（秒）
     */
    public int getSpeechTranscriptionDefaultSeconds() {
        try {
            Map<String, Object> config = getConfigAsMap(KEY_SPEECH_TRANSCRIPTION);
            return getInt(config, "default_seconds");
        } catch (Exception e) {
            return 3600;  // 兜底默认 3600 秒
        }
    }

    /**
     * 获取单条语音最长时长（秒），默认 180 秒（3 分钟）
     */
    public int getSpeechTranscriptionMaxVoiceSeconds() {
        try {
            Map<String, Object> config = getConfigAsMap(KEY_SPEECH_TRANSCRIPTION);
            Integer v = getIntOrNull(config, "max_voice_seconds");
            return v != null && v > 0 ? v : 180;
        } catch (Exception e) {
            return 180;  // 兜底默认 180 秒
        }
    }

    /**
     * 获取板块聊天轮数上限，默认 10 轮。
     * <p>测试时可优先使用 JVM 参数 {@code -Dinterview.max_rounds_per_board=2} 或环境变量 {@code INTERVIEW_MAX_ROUNDS_PER_BOARD=2} 覆盖。</p>
     */
    public int getInterviewMaxRoundsPerBoard() {
        String override = System.getProperty("interview.max_rounds_per_board");
        if (override == null || override.isBlank()) {
            override = System.getenv("INTERVIEW_MAX_ROUNDS_PER_BOARD");
        }
        if (override != null && !override.isBlank()) {
            try {
                int v = Integer.parseInt(override.trim());
                if (v > 0) return v;
            } catch (NumberFormatException ignored) {
                // 忽略无效值，回退 DB
            }
        }
        try {
            Map<String, Object> config = getConfigAsMap(KEY_INTERVIEW);
            Integer v = getIntOrNull(config, "max_rounds_per_board");
            return v != null && v > 0 ? v : 10;
        } catch (Exception e) {
            return 10;  // 兜底默认 10 轮
        }
    }

    /**
     * 获取密码策略配置（不存在时返回默认值）
     */
    public PasswordPolicyConfigDto getPasswordPolicy() {
        return sysConfigRepository.findByConfigKey(KEY_PASSWORD_POLICY)
                .map(SysConfig::getConfigValue)
                .filter(v -> v != null && !v.isEmpty())
                .map(this::mapToPasswordPolicy)
                .orElseGet(PasswordPolicyConfigDto::new);
    }

    /**
     * 更新密码策略配置
     */
    @org.springframework.transaction.annotation.Transactional
    public PasswordPolicyConfigDto updatePasswordPolicy(PasswordPolicyConfigDto dto) {
        SysConfig config = sysConfigRepository.findByConfigKey(KEY_PASSWORD_POLICY)
                .orElseGet(() -> {
                    SysConfig c = new SysConfig();
                    c.setConfigKey(KEY_PASSWORD_POLICY);
                    c.setDescription("密码策略：强制强密码、定期修改密码");
                    return c;
                });
        config.setConfigValue(Map.of(
                "enforceStrongPassword", dto.getEnforceStrongPassword() != null ? dto.getEnforceStrongPassword() : true,
                "requirePasswordChangePeriodically", dto.getRequirePasswordChangePeriodically() != null ? dto.getRequirePasswordChangePeriodically() : false,
                "passwordChangeIntervalDays", dto.getPasswordChangeIntervalDays() != null ? dto.getPasswordChangeIntervalDays() : 90
        ));
        sysConfigRepository.save(config);
        return getPasswordPolicy();
    }

    private PasswordPolicyConfigDto mapToPasswordPolicy(Map<String, Object> map) {
        PasswordPolicyConfigDto dto = new PasswordPolicyConfigDto();
        dto.setEnforceStrongPassword(getBoolean(map, "enforceStrongPassword", true));
        dto.setRequirePasswordChangePeriodically(getBoolean(map, "requirePasswordChangePeriodically", false));
        dto.setPasswordChangeIntervalDays(getIntOrNull(map, "passwordChangeIntervalDays") != null ? getIntOrNull(map, "passwordChangeIntervalDays") : 90);
        return dto;
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object v = map.get(key);
        if (v == null) return defaultValue;
        if (v instanceof Boolean) return (Boolean) v;
        return Boolean.parseBoolean(v.toString());
    }

    private Integer getIntOrNull(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
