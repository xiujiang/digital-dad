package com.digitaldad.ai.websocket;

import com.digitaldad.ai.service.SpeechTranscriptionQuotaService;
import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.user.security.UserPrincipal;
import com.digitaldad.user.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * 语音识别 WebSocket 握手拦截器：JWT 校验 + 配额预检查（HOST/SUPER_ADMIN 豁免）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpeechRecognitionHandshakeInterceptor implements HandshakeInterceptor {

    private static final String TOKEN_PARAM = "token";
    private static final String ATTR_USER_ID = "userId";
    private static final String ATTR_USER_ROLES = "userRoles";

    private final JwtUtils jwtUtils;
    private final SpeechTranscriptionQuotaService quotaService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                  WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        String token = servletRequest.getServletRequest().getParameter(TOKEN_PARAM);
        if (!StringUtils.hasText(token)) {
            log.warn("语音识别 WebSocket 握手失败：缺少 token");
            return false;
        }

        UserPrincipal principal;
        try {
            Long userId = jwtUtils.getUserId(token);
            java.util.Set<String> roles = new java.util.HashSet<>(jwtUtils.getRoles(token));
            principal = new UserPrincipal(userId, roles);
        } catch (Exception e) {
            log.warn("语音识别 WebSocket token 解析失败", e);
            return false;
        }

        try {
            quotaService.checkQuotaForConnect(principal.getUserId(), principal.getRoles());
        } catch (BusinessException e) {
            log.info("语音识别 WebSocket 握手拒绝：{}", e.getMessage());
            return false;
        }

        attributes.put(ATTR_USER_ID, principal.getUserId());
        attributes.put(ATTR_USER_ROLES, principal.getRoles());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception ex) {
        // 无需额外处理
    }
}
