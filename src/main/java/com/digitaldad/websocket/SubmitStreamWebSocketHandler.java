package com.digitaldad.websocket;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.dto.PrepareSubmitStreamResult;
import com.digitaldad.dto.SaveAiReplyResult;
import com.digitaldad.service.AiChatService;
import com.digitaldad.service.InterviewSessionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.Map;

/**
 * 流式提交 WebSocket 处理器
 * <p>客户端连接后发送 JSON {@code {"sessionId": 123}}，服务端先校验并标记提交，再通过 DeepSeek 流式返回 AI 回复；流结束后落库并推送 done。</p>
 * <p>下行消息格式：{@code {"type":"delta","content":"..."}} 增量；{@code {"type":"done","messageId":1,"roundCount":1}} 结束；{@code {"type":"error","message":"..."}} 错误。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubmitStreamWebSocketHandler extends AbstractWebSocketHandler {

    private static final String ATTR_USER_ID = "userId";

    private final InterviewSessionService sessionService;
    private final AiChatService aiChatService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("流式提交 WebSocket 已建立，session={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        if (userId == null) {
            sendError(session, "未认证");
            return;
        }

        Long sessionId;
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            JsonNode sid = root.path("sessionId");
            if (sid.isMissingNode() || !sid.isNumber()) {
                sendError(session, "缺少或无效的 sessionId");
                return;
            }
            sessionId = sid.asLong();
        } catch (Exception e) {
            log.warn("解析 sessionId 失败: {}", message.getPayload(), e);
            sendError(session, "请求格式错误，需要 {\"sessionId\": number}");
            return;
        }

        PrepareSubmitStreamResult prepare;
        try {
            prepare = sessionService.prepareSubmitStream(sessionId, userId);
        } catch (BusinessException e) {
            sendError(session, e.getMessage());
            return;
        } catch (Exception e) {
            log.error("prepareSubmitStream 失败", e);
            sendError(session, "服务异常，请稍后再试");
            return;
        }

        StringBuilder fullContent = new StringBuilder();
        try {
            aiChatService.chatWithHistoryStream(
                    prepare.getSystemPrompt(),
                    prepare.getHistory(),
                    delta -> {
                        if (delta != null && !delta.isEmpty()) {
                            fullContent.append(delta);
                            sendDelta(session, delta);
                        }
                    }
            );
        } catch (Exception e) {
            log.error("AI 流式调用失败", e);
            sendError(session, "AI 回复生成失败，请稍后再试");
            return;
        }

        SaveAiReplyResult saved;
        try {
            saved = sessionService.saveAiReplyAfterStream(sessionId, userId, fullContent.toString(), prepare.getNextBatch());
        } catch (BusinessException e) {
            sendError(session, e.getMessage());
            return;
        } catch (Exception e) {
            log.error("saveAiReplyAfterStream 失败", e);
            sendError(session, "保存回复失败，请稍后再试");
            return;
        }

        sendDone(session, saved.getMessageId(), saved.getRoundCount());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("流式提交 WebSocket 已关闭，session={}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("流式提交 WebSocket 异常，session={}", session.getId(), exception);
    }

    private void sendDelta(WebSocketSession session, String content) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(Map.of("type", "delta", "content", content));
                session.sendMessage(new TextMessage(json));
            }
        } catch (Exception e) {
            log.warn("发送 delta 失败", e);
        }
    }

    private void sendDone(WebSocketSession session, Long messageId, Integer roundCount) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(Map.of(
                        "type", "done",
                        "messageId", messageId != null ? messageId : 0,
                        "roundCount", roundCount != null ? roundCount : 0
                ));
                session.sendMessage(new TextMessage(json));
            }
        } catch (Exception e) {
            log.warn("发送 done 失败", e);
        }
    }

    private void sendError(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(Map.of("type", "error", "message", message != null ? message : "未知错误"));
                session.sendMessage(new TextMessage(json));
            }
        } catch (Exception e) {
            log.warn("发送 error 失败", e);
        }
    }
}
