package com.digitaldad.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 火山引擎语音识别配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.volcengine")
public class VolcengineProperties {
    private String appId = "";
    private String accessToken = "";
    private String resourceId = "volc.seedasr.sauc.duration";
    private String wsUrl = "wss://openspeech.bytedance.com/api/v3/sauc/bigmodel_async";
}
