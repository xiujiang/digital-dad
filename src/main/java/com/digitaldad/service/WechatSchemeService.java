package com.digitaldad.service;

import com.digitaldad.common.config.WeChatMiniprogramProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

/**
 * 微信小程序 URL Scheme 生成服务
 * <p>用于分享到微信时生成「打开小程序并带参数」的 Scheme（weixin://dl/business/?t=xxx），
 * 微信内会展示为 #小程序://... 形式。</p>
 */
@Slf4j
@Service
public class WechatSchemeService {

    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String GENERATE_SCHEME_URL = "https://api.weixin.qq.com/wxa/generatescheme";
    /** 小程序落地页 path，与前端约定：扫码/链接进入的页面 */
    private static final String ENTRY_PATH = "/pages/interview/index";
    /** Scheme 有效天数（微信最长 30 天） */
    private static final int EXPIRE_DAYS = 30;
    /** access_token 提前刷新缓冲（秒） */
    private static final int TOKEN_REFRESH_BUFFER_SEC = 300;

    private final WeChatMiniprogramProperties wechatProps;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private volatile String cachedAccessToken;
    private volatile long tokenExpiresAtMs;

    public WechatSchemeService(WeChatMiniprogramProperties wechatProps) {
        this.wechatProps = wechatProps;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 为指定 shareToken 生成小程序 URL Scheme。
     * <p>若未配置 appId/appSecret 或微信接口失败，返回 empty。</p>
     */
    public Optional<String> generateSchemeForShareToken(String shareToken) {
        if (shareToken == null || shareToken.isBlank()) {
            return Optional.empty();
        }
        if (wechatProps.getAppId() == null || wechatProps.getAppId().isBlank()
                || wechatProps.getAppSecret() == null || wechatProps.getAppSecret().isBlank()) {
            log.info("微信小程序未配置 appId/appSecret，跳过 Scheme 生成（wechatScheme 为 null）；请在 app.wechat.miniprogram 或环境变量 WECHAT_MINI_APP_ID/WECHAT_MINI_APP_SECRET 中配置");
            return Optional.empty();
        }

        Optional<String> tokenOpt = getAccessToken();
        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        String query = "token=" + shareToken.trim();
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "jump_wxa", Map.of(
                            "path", ENTRY_PATH,
                            "query", query,
                            "env_version", "release"
                    ),
                    "expire_type", 1,
                    "expire_interval", EXPIRE_DAYS
            ));
            String url = GENERATE_SCHEME_URL + "?access_token=" + tokenOpt.get();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> resp = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(body, headers),
                    String.class
            );
            JsonNode root = objectMapper.readTree(resp.getBody());
            int errcode = root.path("errcode").asInt(0);
            if (errcode != 0) {
                String errmsg = root.path("errmsg").asText("");
                log.warn("微信 generateScheme 失败: errcode={}, errmsg={}（若为 40165 多为 path 未发布，请确保小程序已发布 /pages/interview/index）", errcode, errmsg);
                return Optional.empty();
            }
            String openlink = root.path("openlink").asText(null);
            if (openlink == null || openlink.isBlank()) {
                log.warn("微信 generateScheme 返回无 openlink");
                return Optional.empty();
            }
            return Optional.of(openlink);
        } catch (Exception e) {
            log.warn("调用微信 generateScheme 异常: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> getAccessToken() {
        long now = System.currentTimeMillis();
        if (cachedAccessToken != null && now < tokenExpiresAtMs) {
            return Optional.of(cachedAccessToken);
        }
        synchronized (this) {
            if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpiresAtMs) {
                return Optional.of(cachedAccessToken);
            }
            try {
                String url = TOKEN_URL + "?grant_type=client_credential"
                        + "&appid=" + wechatProps.getAppId()
                        + "&secret=" + wechatProps.getAppSecret();
                ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
                JsonNode root = objectMapper.readTree(resp.getBody());
                if (root.has("errcode") && root.get("errcode").asInt(0) != 0) {
                    int code = root.path("errcode").asInt();
                    String errmsg = root.path("errmsg").asText("");
                    log.warn("微信 getAccessToken 失败: errcode={}, errmsg={}（请检查 appId/appSecret 是否正确）", code, errmsg);
                    return Optional.empty();
                }
                String token = root.path("access_token").asText(null);
                int expiresIn = root.path("expires_in").asInt(7200);
                if (token == null || token.isBlank()) {
                    return Optional.empty();
                }
                cachedAccessToken = token;
                tokenExpiresAtMs = System.currentTimeMillis() + (expiresIn - TOKEN_REFRESH_BUFFER_SEC) * 1000L;
                return Optional.of(token);
            } catch (Exception e) {
                log.warn("获取微信 access_token 异常: {}", e.getMessage());
                return Optional.empty();
            }
        }
    }

}
