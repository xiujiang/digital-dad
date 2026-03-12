package com.digitaldad.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** 密钥，至少32字符(256位) */
    private String secret = "digital-dad-jwt-secret-key-at-least-256-bits";

    /** 过期时间(秒)，默认24小时 */
    private long expirationSeconds = 86400;

    /** 请求头名称 */
    private String header = "Authorization";

    /** Token 前缀 */
    private String prefix = "Bearer ";
}
