package com.digitaldad.websocket;

import com.digitaldad.util.JwtUtils;
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
 * 流式提交 WebSocket 握手拦截器：仅 JWT 校验，将 userId 写入 attributes
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubmitStreamHandshakeInterceptor implements HandshakeInterceptor {

    private static final String TOKEN_PARAM = "token";
    private static final String ATTR_USER_ID = "userId";

    private final JwtUtils jwtUtils;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                  WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        String token = servletRequest.getServletRequest().getParameter(TOKEN_PARAM);
        if (!StringUtils.hasText(token)) {
            log.warn("流式提交 WebSocket 握手失败：缺少 token");
            return false;
        }
        try {
            Long userId = jwtUtils.getUserId(token);
            attributes.put(ATTR_USER_ID, userId);
            return true;
        } catch (Exception e) {
            log.warn("流式提交 WebSocket token 解析失败", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception ex) {
        // 无需额外处理
    }
}
