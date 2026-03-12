package com.digitaldad.user.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.user.enums.SmsScene;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 短信验证码服务（v0.1 使用内存存储，模拟发送）
 */
@Slf4j
@Service
public class SmsService {

    private static final String KEY_CODE = "sms:code:";
    private static final String KEY_LIMIT = "sms:limit:";
    private static final int CODE_EXPIRE_SECONDS = 300;  // 5分钟
    private static final int LIMIT_SECONDS = 60;         // 60秒内只能发一次

    private final ConcurrentHashMap<String, CodeEntry> codeStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> limitStore = new ConcurrentHashMap<>();

    private static class CodeEntry {
        final String code;
        final long expiryAt;

        CodeEntry(String code, long expiryAt) {
            this.code = code;
            this.expiryAt = expiryAt;
        }
    }

    /**
     * 发送验证码（v0.1 内存存储，模拟发送）
     *
     * @param phone 手机号
     * @param scene 场景（如 LOGIN）
     */
    public void sendCode(String phone, SmsScene scene) {
        String limitKey = KEY_LIMIT + scene.name() + ":" + phone;
        long now = System.currentTimeMillis();
        Long limitExpiry = limitStore.get(limitKey);
        if (limitExpiry != null && now < limitExpiry) {
            throw new BusinessException(429, "操作过于频繁，请60秒后再试");
        }

        String code = String.format("%06d", (int) (Math.random() * 1000000));
        String codeKey = KEY_CODE + scene.name() + ":" + phone;

        codeStore.put(codeKey, new CodeEntry(code, now + CODE_EXPIRE_SECONDS * 1000L));
        limitStore.put(limitKey, now + LIMIT_SECONDS * 1000L);

        // v0.1 模拟发送，仅打印日志便于测试
        log.info("[SMS] 验证码已发送 phone={}, scene={}, code={}", phone, scene, code);
    }

    /**
     * 校验验证码（校验成功后验证码失效）
     *
     * @param phone 手机号
     * @param scene 场景
     * @param code  验证码
     * @return 是否校验通过
     */
    public boolean verifyCode(String phone, SmsScene scene, String code) {
        String codeKey = KEY_CODE + scene.name() + ":" + phone;
        CodeEntry entry = codeStore.get(codeKey);
        if (entry == null || System.currentTimeMillis() > entry.expiryAt) {
            return false;
        }
        boolean match = entry.code.equals(code);
        if (match) {
            codeStore.remove(codeKey);
        }
        return match;
    }
}
