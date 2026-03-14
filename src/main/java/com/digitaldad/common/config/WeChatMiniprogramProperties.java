package com.digitaldad.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置（用于 C 端扫码登录：code2session 换 openid）
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.wechat.miniprogram")
public class WeChatMiniprogramProperties {

    private String appId = "";
    private String appSecret = "";
}
