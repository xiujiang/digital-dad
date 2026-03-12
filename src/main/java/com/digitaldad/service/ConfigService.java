package com.digitaldad.config.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.config.dto.MemberPackageConfigDto;
import com.digitaldad.config.entity.SysConfig;
import com.digitaldad.config.repository.SysConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    private final SysConfigRepository sysConfigRepository;

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
     * 获取板块聊天轮数上限，默认 10 轮
     */
    public int getInterviewMaxRoundsPerBoard() {
        try {
            Map<String, Object> config = getConfigAsMap(KEY_INTERVIEW);
            Integer v = getIntOrNull(config, "max_rounds_per_board");
            return v != null && v > 0 ? v : 10;
        } catch (Exception e) {
            return 10;  // 兜底默认 10 轮
        }
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
