package com.digitaldad.service;

import com.digitaldad.common.config.WeChatMiniprogramProperties;
import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.dto.WeChatCode2SessionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 微信小程序 code2session：用 code 换 openid/session_key
 * <p>前端 wx.login() 得到 code 后由后端调用此服务，再根据 openid 查/建用户并签发 JWT。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeChatMiniProgramService {

    private static final String JSCODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    private final WeChatMiniprogramProperties wechatProps;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 用小程序临时 code 换取 openid（及 session_key、unionid）
     *
     * @param code 前端 wx.login() 得到的 code
     * @return 含 openid、sessionKey、unionid（成功时）
     * @throws BusinessException code 无效/过期或微信接口错误
     */
    public WeChatCode2SessionResponse code2session(String code) {
        String appId = wechatProps.getAppId();
        String appSecret = wechatProps.getAppSecret();
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            log.warn("微信小程序 appId/appSecret 未配置，无法执行 code2session");
            throw new BusinessException(500, "微信登录未配置，请联系管理员");
        }

        String url = UriComponentsBuilder.fromHttpUrl(JSCODE2SESSION_URL)
                .queryParam("appid", appId)
                .queryParam("secret", appSecret)
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .build()
                .toUriString();

        // 微信接口可能返回 Content-Type: text/plain，RestTemplate 默认无法反序列化，故先取字符串再手动解析
        String responseBody;
        try {
            responseBody = restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            log.error("调用微信 jscode2session 异常: {}", e.getMessage());
            throw new BusinessException(503, "微信服务暂时不可用，请稍后重试");
        }
        if (responseBody == null || responseBody.isBlank()) {
            throw new BusinessException(503, "微信登录失败，请重试");
        }
        WeChatCode2SessionResponse resp;
        try {
            resp = objectMapper.readValue(responseBody, WeChatCode2SessionResponse.class);
        } catch (Exception e) {
            log.error("解析微信 jscode2session 响应失败: {}", e.getMessage());
            throw new BusinessException(503, "微信登录失败，请重试");
        }

        if (resp == null) {
            throw new BusinessException(503, "微信登录失败，请重试");
        }

        if (resp.getErrcode() != null && resp.getErrcode() != 0) {
            log.warn("微信 code2session 失败: errcode={}, errmsg={}", resp.getErrcode(), resp.getErrmsg());
            // 40029 code 无效, 40163 code 已被使用
            if (Integer.valueOf(40029).equals(resp.getErrcode()) || Integer.valueOf(40163).equals(resp.getErrcode())) {
                throw new BusinessException(401, "登录已过期，请重新打开小程序");
            }
            throw new BusinessException(401, resp.getErrmsg() != null ? resp.getErrmsg() : "微信登录失败，请重试");
        }

        if (resp.getOpenid() == null || resp.getOpenid().isBlank()) {
            throw new BusinessException(503, "微信登录失败，未获取到用户标识");
        }

        return resp;
    }
}
