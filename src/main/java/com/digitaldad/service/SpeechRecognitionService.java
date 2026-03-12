package com.digitaldad.ai.service;

import com.digitaldad.ai.config.VolcengineProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 火山引擎流式语音识别服务
 * <p>通过 WebSocket 连接火山引擎，实时转写语音流，支持 PCM 音频发送与增量转写回调。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechRecognitionService {

    private final VolcengineProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 建立与火山引擎的 WebSocket 连接，并启动实时转写
     *
     * @param onTranscript 收到转写结果时回调（增量或完整文本）
     * @param onError      发生错误时回调
     * @return WebSocket 连接包装，用于发送音频和关闭
     */
    public VolcAsrConnection createConnection(Consumer<String> onTranscript, Consumer<Throwable> onError) {
        OkHttpClient client = new OkHttpClient.Builder()
                .pingInterval(java.time.Duration.ofSeconds(30))
                .build();

        String connectId = UUID.randomUUID().toString();

        Request request = new Request.Builder()
                .url(props.getWsUrl())
                .addHeader("X-Api-App-Key", props.getAppId())
                .addHeader("X-Api-Access-Key", props.getAccessToken())
                .addHeader("X-Api-Resource-Id", props.getResourceId())
                .addHeader("X-Api-Connect-Id", connectId)
                .build();

        WebSocket ws = client.newWebSocket(request, new VolcAsrListener(onTranscript, onError, props.getWsUrl()));

        return new VolcAsrConnection(ws, connectId);
    }

    /**
     * 火山引擎 ASR 连接包装
     * 方案 B：仅统计已成功转发到火山的 PCM 字节，用于扣减配额
     */
    public static class VolcAsrConnection {
        private final WebSocket webSocket;
        private final String connectId;
        private boolean fullRequestSent = false;
        private final Object lock = new Object();
        private final AtomicLong bytesForwardedToVolcano = new AtomicLong(0);

        public VolcAsrConnection(WebSocket webSocket, String connectId) {
            this.webSocket = webSocket;
            this.connectId = connectId;
        }

        /** 获取已成功转发到火山引擎的 PCM 字节数 */
        public long getBytesForwardedToVolcano() {
            return bytesForwardedToVolcano.get();
        }

        /** 获取连接 ID */
        public String getConnectId() {
            return connectId;
        }

        /**
         * 发送 full client request（首次建连后必须调用）
         */
        public void sendFullRequest(byte[] jsonPayload) {
            synchronized (lock) {
                if (fullRequestSent) return;
                byte[] frame = buildFullRequestFrame(jsonPayload);
                webSocket.send(okio.ByteString.of(frame));
                fullRequestSent = true;
            }
        }

        /**
         * 发送音频数据（PCM 16k 16bit mono）
         * 方案 B：成功发送后累加字节数，用于配额扣减
         */
        public void sendAudio(byte[] audioData) {
            if (!fullRequestSent) {
                log.warn("未发送 full request，无法发送音频");
                return;
            }
            byte[] frame = buildAudioFrame(audioData);
            webSocket.send(okio.ByteString.of(frame));
            bytesForwardedToVolcano.addAndGet(audioData.length);
        }

        /**
         * 发送负包，表示音频结束
         */
        public void sendEndPacket() {
            byte[] frame = buildEndFrame();
            webSocket.send(okio.ByteString.of(frame));
        }

        public void close() {
            webSocket.close(1000, "Normal closure");
        }

        private byte[] buildFullRequestFrame(byte[] jsonPayload) {
            ByteBuffer buf = ByteBuffer.allocate(8 + jsonPayload.length);
            buf.order(ByteOrder.BIG_ENDIAN);
            buf.put((byte) 0x11);  // version 1, header size 4
            buf.put((byte) 0x10);  // full client request, flags 0
            buf.put((byte) 0x10);  // JSON, no compression
            buf.put((byte) 0);
            buf.putInt(jsonPayload.length);
            buf.put(jsonPayload);
            return buf.array();
        }

        private byte[] buildAudioFrame(byte[] audioData) {
            ByteBuffer buf = ByteBuffer.allocate(8 + audioData.length);
            buf.order(ByteOrder.BIG_ENDIAN);
            buf.put((byte) 0x11);
            buf.put((byte) 0x20);  // audio only request
            buf.put((byte) 0x00);  // raw, no compression
            buf.put((byte) 0);
            buf.putInt(audioData.length);
            buf.put(audioData);
            return buf.array();
        }

        private byte[] buildEndFrame() {
            // 负包：audio only + 最后一包标志，空 payload
            ByteBuffer buf = ByteBuffer.allocate(8);
            buf.order(ByteOrder.BIG_ENDIAN);
            buf.put((byte) 0x11);
            buf.put((byte) 0x22);  // audio only + 最后一包 (flags 0b0010)
            buf.put((byte) 0x00);
            buf.put((byte) 0);
            buf.putInt(0);
            return buf.array();
        }
    }

    /**
     * 构建 full client request 的 JSON payload
     */
    public byte[] buildFullRequestJson() {
        String json = """
                {
                  "audio": {
                    "format": "pcm",
                    "rate": 16000,
                    "bits": 16,
                    "channel": 1
                  },
                  "request": {
                    "model_name": "bigmodel",
                    "enable_punc": true,
                    "enable_itn": true
                  }
                }
                """;
        return json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static class VolcAsrListener extends WebSocketListener {
        private final Consumer<String> onTranscript;
        private final Consumer<Throwable> onError;
        private final String wsUrl;

        public VolcAsrListener(Consumer<String> onTranscript, Consumer<Throwable> onError, String wsUrl) {
            this.onTranscript = onTranscript;
            this.onError = onError;
            this.wsUrl = wsUrl;
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            log.info("火山引擎 ASR WebSocket 已连接");
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            // 火山引擎通常用二进制，此处备用
        }

        @Override
        public void onMessage(WebSocket webSocket, okio.ByteString bytes) {
            try {
                String text = parseServerResponse(bytes.toByteArray());
                if (text != null && !text.isBlank()) {
                    onTranscript.accept(text);
                }
            } catch (Exception e) {
                log.warn("解析 ASR 响应失败", e);
            }
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            log.error("火山引擎 ASR WebSocket 异常", t);
            onError.accept(t);
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            log.info("火山引擎 ASR WebSocket 关闭: {} {}", code, reason);
        }

        private String parseServerResponse(byte[] data) throws Exception {
            if (data.length < 8) return null;
            // header 4 bytes + payload_size 4 bytes (big endian)
            int payloadSize = ((data[4] & 0xFF) << 24) | ((data[5] & 0xFF) << 16)
                    | ((data[6] & 0xFF) << 8) | (data[7] & 0xFF);
            if (data.length < 8 + payloadSize) return null;

            byte[] payload = new byte[payloadSize];
            System.arraycopy(data, 8, payload, 0, payloadSize);

            JsonNode root = new ObjectMapper().readTree(new String(payload, java.nio.charset.StandardCharsets.UTF_8));
            JsonNode result = root.path("result");
            if (result.isMissingNode()) return null;
            JsonNode textNode = result.path("text");
            return textNode.asText(null);
        }
    }
}
