package com.digitaldad.websocket;

import com.digitaldad.service.SpeechTranscriptionQuotaService;
import com.digitaldad.service.SpeechRecognitionService;
import com.digitaldad.service.ConfigService;
import java.util.Set;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 语音识别 WebSocket 处理器
 * 接收客户端发送的 PCM 音频流，转发至火山引擎，实时回传转写结果
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpeechRecognitionWebSocketHandler extends AbstractWebSocketHandler {

    private static final String ATTR_USER_ID = "userId";
    private static final String ATTR_USER_ROLES = "userRoles";
    private static final String ATTR_BYTES_RECEIVED = "bytesReceived";
    private static final int BYTES_PER_SECOND = 32000;  // 16k × 16bit × 1ch

    private final SpeechRecognitionService speechService;
    private final SpeechTranscriptionQuotaService quotaService;
    private final ConfigService configService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        SpeechRecognitionService.VolcAsrConnection conn = speechService.createConnection(
                transcript -> sendTranscript(session, transcript),
                error -> sendError(session, error)
        );
        session.getAttributes().put("volcConnection", conn);

        conn.sendFullRequest(speechService.buildFullRequestJson());
        log.info("语音识别 WebSocket 已建立，session={}", session.getId());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        SpeechRecognitionService.VolcAsrConnection conn = getConnection(session);
        if (conn == null) return;

        var buf = message.getPayload();
        byte[] payload = new byte[buf.remaining()];
        buf.get(payload);

        int maxSeconds = configService.getSpeechTranscriptionMaxVoiceSeconds();
        long maxBytes = (long) maxSeconds * BYTES_PER_SECOND;
        AtomicLong received = (AtomicLong) session.getAttributes().computeIfAbsent(ATTR_BYTES_RECEIVED, k -> new AtomicLong(0));

        if (received.get() >= maxBytes) {
            sendError(session, new RuntimeException("单条语音最长 " + maxSeconds + " 秒，已到达上限"));
            return;
        }
        if (received.get() + payload.length > maxBytes) {
            sendError(session, new RuntimeException("单条语音最长 " + maxSeconds + " 秒，已到达上限"));
            return;
        }

        received.addAndGet(payload.length);
        log.info("收到前端 Binary: {} 字节, 本会话累计: {} 字节", payload.length, received.get());
        conn.sendAudio(payload);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("收到前端 Text: [{}]", payload);
        if ("end".equals(payload) || "stop".equals(payload)) {
            SpeechRecognitionService.VolcAsrConnection conn = getConnection(session);
            if (conn != null) {
                conn.sendEndPacket();
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        performQuotaDeduct(session);
        SpeechRecognitionService.VolcAsrConnection conn = getConnection(session);
        if (conn != null) {
            conn.close();
        }
        AtomicLong received = (AtomicLong) session.getAttributes().get(ATTR_BYTES_RECEIVED);
        long totalReceived = received != null ? received.get() : 0;
        log.info("语音识别 WebSocket 已关闭，session={}, 本会话累计收到前端 {} 字节", session.getId(), totalReceived);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("语音识别 WebSocket 异常，session={}", session.getId(), exception);
        performQuotaDeduct(session);
        SpeechRecognitionService.VolcAsrConnection conn = getConnection(session);
        if (conn != null) {
            conn.close();
        }
    }

    /**
     * 方案 B：按已成功转发到火山的字节数扣减配额，HOST/SUPER_ADMIN 豁免
     */
    private void performQuotaDeduct(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        @SuppressWarnings("unchecked")
        Set<String> roles = (Set<String>) session.getAttributes().get(ATTR_USER_ROLES);
        if (userId == null || (roles != null && (roles.contains("HOST") || roles.contains("SUPER_ADMIN")))) {
            return;
        }
        SpeechRecognitionService.VolcAsrConnection conn = getConnection(session);
        if (conn == null) return;
        long bytes = conn.getBytesForwardedToVolcano();
        if (bytes <= 0) return;
        try {
            quotaService.deduct(userId, bytes, conn.getConnectId());
        } catch (Exception e) {
            log.warn("语音转写配额扣减失败: userId={}", userId, e);
        }
    }

    @SuppressWarnings("unchecked")
    private SpeechRecognitionService.VolcAsrConnection getConnection(WebSocketSession session) {
        return (SpeechRecognitionService.VolcAsrConnection) session.getAttributes().get("volcConnection");
    }

    private void sendTranscript(WebSocketSession session, String transcript) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(Map.of("type", "transcript", "text", transcript));
                log.info("推送给前端: {}", json);
                session.sendMessage(new TextMessage(json));
            }
        } catch (Exception e) {
            log.warn("发送转写结果失败", e);
        }
    }

    private void sendError(WebSocketSession session, Throwable error) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(Map.of(
                        "type", "error",
                        "message", error != null ? error.getMessage() : "未知错误"
                ));
                session.sendMessage(new TextMessage(json));
            }
        } catch (Exception e) {
            log.warn("发送错误信息失败", e);
        }
    }
}
