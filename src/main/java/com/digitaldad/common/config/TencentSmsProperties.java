package com.digitaldad.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 腾讯云短信配置
 * <p>SecretId 和 SecretKey 在腾讯云控制台 -> 访问管理 -> API密钥管理 获取</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "sms.tencent")
public class TencentSmsProperties {

    /** 腾讯云 API 密钥 ID（必填，与 SecretKey 成对） */
    private String secretId = "";

    /** 腾讯云 API 密钥（必填，与 SecretId 成对，短信签名中的 app Key 非此值） */
    private String secretKey = "";

    /** 短信应用 SDKAppID */
    private String sdkAppId = "1401093377";

    /** 短信签名内容 */
    private String signName = "舟山市新城踏浪文化";

    /** 验证码模板 ID */
    private String templateId = "2609865";

    /** API 地域，默认 ap-guangzhou */
    private String region = "ap-guangzhou";

    /**
     * 是否已配置完整（可真实发送短信）
     */
    public boolean isEnabled() {
        return secretId != null && !secretId.isBlank()
                && secretKey != null && !secretKey.isBlank()
                && sdkAppId != null && !sdkAppId.isBlank()
                && signName != null && !signName.isBlank()
                && templateId != null && !templateId.isBlank();
    }
}
