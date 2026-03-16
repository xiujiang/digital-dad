package com.digitaldad.service;

import com.digitaldad.common.config.TencentSmsProperties;
import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.enums.SmsScene;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 短信验证码服务
 * <p>配置了腾讯云 SecretId/SecretKey 时真实发送，未配置时仅打印日志（便于开发测试）</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private static final String KEY_CODE = "sms:code:";
    private static final String KEY_LIMIT = "sms:limit:";
    private static final int CODE_EXPIRE_SECONDS = 300;  // 5分钟
    private static final int LIMIT_SECONDS = 60;         // 60秒内只能发一次

    private final TencentSmsProperties tencentSmsProps;
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
     * 发送验证码
     * <p>配置了腾讯云则真实发送，否则仅打印日志</p>
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

        if (tencentSmsProps.isEnabled()) {
            doSendTencentSms(phone, code);
        } else {
            log.info("[SMS] 模拟发送（未配置腾讯云） phone={}, scene={}, code={}", phone, scene, code);
        }
    }

    private void doSendTencentSms(String phone, String code) {
        try {
            String e164 = normalizePhone(phone);
            Credential cred = new Credential(tencentSmsProps.getSecretId(), tencentSmsProps.getSecretKey());
            SmsClient client = new SmsClient(cred, tencentSmsProps.getRegion());

            SendSmsRequest req = new SendSmsRequest();
            req.setPhoneNumberSet(new String[]{e164});
            req.setSmsSdkAppId(tencentSmsProps.getSdkAppId());
            req.setSignName(tencentSmsProps.getSignName());
            req.setTemplateId(tencentSmsProps.getTemplateId());
            int paramCount = tencentSmsProps.getTemplateParamCount();
            String[] templateParams = paramCount >= 2
                    ? new String[]{code, String.valueOf(CODE_EXPIRE_SECONDS / 60)}
                    : new String[]{code};
            req.setTemplateParamSet(templateParams);

            SendSmsResponse resp = client.SendSms(req);
            if (resp.getSendStatusSet() != null && resp.getSendStatusSet().length > 0) {
                String errCode = resp.getSendStatusSet()[0].getCode();
                if (!"Ok".equalsIgnoreCase(errCode)) {
                    String errMsg = resp.getSendStatusSet()[0].getMessage();
                    log.error("[SMS] 腾讯云发送失败 phone={}, errCode={}, errMsg={}", phone, errCode, errMsg);
                    throw new BusinessException(500, "短信发送失败: " + errMsg);
                }
            }
            log.info("[SMS] 验证码已发送 phone={}", phone);
        } catch (TencentCloudSDKException e) {
            log.error("[SMS] 腾讯云 SDK 异常 phone={}", phone, e);
            throw new BusinessException(500, "短信发送失败，请稍后重试");
        }
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        String s = phone.trim().replaceAll("\\s+", "");
        if (s.startsWith("+86")) return s;
        if (s.startsWith("86") && s.length() > 10) return "+" + s;
        return "+86" + s;
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
