package com.digitaldad.ai.config;

import com.digitaldad.ai.websocket.SpeechRecognitionHandshakeInterceptor;
import com.digitaldad.ai.websocket.SpeechRecognitionWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final SpeechRecognitionWebSocketHandler speechRecognitionHandler;
    private final SpeechRecognitionHandshakeInterceptor handshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(speechRecognitionHandler, "/api/c/ws/speech-recognition")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");  // 生产环境建议限制具体域名
    }
}
