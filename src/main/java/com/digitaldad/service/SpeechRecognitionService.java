package com.digitaldad.service;

import com.digitaldad.common.config.VolcengineProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 火山引擎流式语音识别服务（对齐官方 sauc demo 协议）
 * <p>通过 WebSocket 连接火山引擎，实时转写语音流；帧格式为 Header(4)+Sequence(4)+PayloadSize(4)+Payload，请求使用 GZIP。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechRecognitionService {

    // 协议常量（与 sauc demo 一致）
    private static final byte PROTOCOL_VERSION = 0b0001;
    private static final byte DEFAULT_HEADER_SIZE = 0b0001;
    private static final byte CLIENT_FULL_REQUEST = 0b0001;
    private static final byte CLIENT_AUDIO_ONLY_REQUEST = 0b0010;
    private static final byte SERVER_FULL_RESPONSE = 0b1001;
    private static final byte SERVER_ERROR_RESPONSE = 0b1111;
    private static final byte NO_SEQUENCE = 0b0000;
    private static final byte POS_SEQUENCE = 0b0001;
    private static final byte NEG_SEQUENCE_ONLY = 0b0010;
    private static final byte NEG_WITH_SEQUENCE = 0b0011;
    private static final byte NO_SERIALIZATION = 0b0000;
    private static final byte JSON_SERIAL = 0b0001;
    private static final byte NO_COMPRESS = 0b0000;
    private static final byte GZIP_COMPRESS = 0b0001;

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
        /** 与 sauc demo 一致：full request 用 seq=1，之后音频递增，end 包用负 seq */
        private final AtomicInteger nextSeq = new AtomicInteger(1);

        public VolcAsrConnection(WebSocket webSocket, String connectId) {
            this.webSocket = webSocket;
            this.connectId = connectId;
        }

        public long getBytesForwardedToVolcano() {
            return bytesForwardedToVolcano.get();
        }

        public String getConnectId() {
            return connectId;
        }

        public void sendFullRequest(byte[] jsonPayload) {
            synchronized (lock) {
                if (fullRequestSent) return;
                byte[] frame = buildFullRequestFrame(jsonPayload);
                webSocket.send(okio.ByteString.of(frame));
                fullRequestSent = true;
                nextSeq.set(2);  // 下一包音频从 2 开始
            }
        }

        public void sendAudio(byte[] audioData) {
            if (!fullRequestSent) {
                log.warn("未发送 full request，无法发送音频");
                return;
            }
            int seq = nextSeq.getAndIncrement();
            byte[] frame = buildAudioFrame(audioData, seq, false);
            webSocket.send(okio.ByteString.of(frame));
            bytesForwardedToVolcano.addAndGet(audioData.length);
        }

        public void sendEndPacket() {
            int seq = -nextSeq.getAndIncrement();
            byte[] frame = buildEndFrame(seq);
            webSocket.send(okio.ByteString.of(frame));
        }

        public void close() {
            webSocket.close(1000, "Normal closure");
        }

        private byte[] buildFullRequestFrame(byte[] jsonPayload) {
            try {
                byte[] compressed = gzipCompress(jsonPayload);
                byte[] header = getHeader(CLIENT_FULL_REQUEST, POS_SEQUENCE, JSON_SERIAL, GZIP_COMPRESS);
                byte[] seqBytes = intToBytesBigEndian(1);
                byte[] sizeBytes = intToBytesBigEndian(compressed.length);
                byte[] frame = new byte[header.length + seqBytes.length + sizeBytes.length + compressed.length];
                int off = 0;
                System.arraycopy(header, 0, frame, off, header.length); off += header.length;
                System.arraycopy(seqBytes, 0, frame, off, seqBytes.length); off += seqBytes.length;
                System.arraycopy(sizeBytes, 0, frame, off, sizeBytes.length); off += sizeBytes.length;
                System.arraycopy(compressed, 0, frame, off, compressed.length);
                return frame;
            } catch (IOException e) {
                throw new RuntimeException("GZIP full request 失败", e);
            }
        }

        private byte[] buildAudioFrame(byte[] audioData, int seq, boolean isLast) {
            try {
                byte[] compressed = gzipCompress(audioData);
                byte flags = isLast ? NEG_WITH_SEQUENCE : POS_SEQUENCE;
                // 与 sauc demo 一致：音频包也用 JSON 序列化类型（协议文档写 raw，但 demo 用 JSON 且可收到结果）
                byte[] header = getHeader(CLIENT_AUDIO_ONLY_REQUEST, flags, JSON_SERIAL, GZIP_COMPRESS);
                byte[] seqBytes = intToBytesBigEndian(seq);
                byte[] sizeBytes = intToBytesBigEndian(compressed.length);
                byte[] frame = new byte[header.length + seqBytes.length + sizeBytes.length + compressed.length];
                int off = 0;
                System.arraycopy(header, 0, frame, off, header.length); off += header.length;
                System.arraycopy(seqBytes, 0, frame, off, seqBytes.length); off += seqBytes.length;
                System.arraycopy(sizeBytes, 0, frame, off, sizeBytes.length); off += sizeBytes.length;
                System.arraycopy(compressed, 0, frame, off, compressed.length);
                return frame;
            } catch (IOException e) {
                throw new RuntimeException("GZIP audio 失败", e);
            }
        }

        private byte[] buildEndFrame(int negSeq) {
            // 与 demo 一致：结束包也使用 JSON + 无压缩（与 full request 的 serial 一致便于服务端解析）
            byte[] header = getHeader(CLIENT_AUDIO_ONLY_REQUEST, NEG_WITH_SEQUENCE, JSON_SERIAL, NO_COMPRESS);
            byte[] seqBytes = intToBytesBigEndian(negSeq);
            byte[] sizeBytes = intToBytesBigEndian(0);
            byte[] frame = new byte[header.length + seqBytes.length + sizeBytes.length];
            int off = 0;
            System.arraycopy(header, 0, frame, off, header.length); off += header.length;
            System.arraycopy(seqBytes, 0, frame, off, seqBytes.length); off += seqBytes.length;
            System.arraycopy(sizeBytes, 0, frame, off, sizeBytes.length);
            return frame;
        }
    }

    private static byte[] getHeader(byte messageType, byte messageTypeFlags, byte serial, byte compression) {
        return new byte[] {
                (byte) ((PROTOCOL_VERSION << 4) | DEFAULT_HEADER_SIZE),
                (byte) ((messageType << 4) | messageTypeFlags),
                (byte) ((serial << 4) | compression),
                (byte) 0
        };
    }

    private static byte[] intToBytesBigEndian(int v) {
        return new byte[] {
                (byte) ((v >> 24) & 0xFF),
                (byte) ((v >> 16) & 0xFF),
                (byte) ((v >> 8) & 0xFF),
                (byte) (v & 0xFF)
        };
    }

    private static byte[] gzipCompress(byte[] src) throws IOException {
        if (src == null || src.length == 0) return new byte[0];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(src);
        }
        return out.toByteArray();
    }

    private static byte[] gzipDecompress(byte[] src) throws IOException {
        if (src == null || src.length == 0) return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(src))) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = gzip.read(buf)) > 0) out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    /**
     * 构建 full client request 的 JSON payload（与 sauc demo 对齐）
     */
    public byte[] buildFullRequestJson() {
        String json = """
                {
                  "user": { "uid": "digital_dad_server" },
                  "audio": {
                    "format": "pcm",
                    "codec": "raw",
                    "rate": 16000,
                    "bits": 16,
                    "channel": 1
                  },
                  "request": {
                    "model_name": "bigmodel",
                    "enable_itn": true,
                    "enable_punc": true,
                    "enable_ddc": true,
                    "show_utterances": true,
                    "enable_nonstream": false
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
                byte[] data = bytes.toByteArray();
                log.info("火山下行 收到一帧, 总长={} 字节", data.length);
                String text = parseServerResponse(data);
                if (text != null && !text.isBlank()) {
                    log.info("火山引擎识别: {}", text);
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

        /**
         * 解析下行（与 sauc demo 一致）：Header(4) + [Sequence(4) 若 flag] + PayloadSize(4) + Payload，支持 GZIP。
         */
        private String parseServerResponse(byte[] res) throws Exception {
            if (res == null || res.length < 4) return null;

            int headerSize = res[0] & 0x0F;
            int messageType = (res[1] >> 4) & 0x0F;
            int messageTypeFlags = res[1] & 0x0F;
            int compression = res[2] & 0x0F;

            int payloadOffset = headerSize * 4;
            if (res.length <= payloadOffset) return null;

            byte[] payload = Arrays.copyOfRange(res, payloadOffset, res.length);

            if ((messageTypeFlags & 0x01) != 0) {
                if (payload.length < 4) return null;
                payload = Arrays.copyOfRange(payload, 4, payload.length);
            }
            if ((messageTypeFlags & 0x02) != 0) {
                // isLastPackage，可忽略
            }
            if ((messageTypeFlags & 0x04) != 0) {
                if (payload.length < 4) return null;
                payload = Arrays.copyOfRange(payload, 4, payload.length);
            }

            switch (messageType) {
                case SERVER_FULL_RESPONSE:
                    if (payload.length < 4) return null;
                    int payloadLen = ((payload[0] & 0xFF) << 24) | ((payload[1] & 0xFF) << 16)
                            | ((payload[2] & 0xFF) << 8) | (payload[3] & 0xFF);
                    if (payloadLen <= 0 || payload.length < 4 + payloadLen) return null;
                    payload = Arrays.copyOfRange(payload, 4, 4 + payloadLen);
                    break;
                case SERVER_ERROR_RESPONSE:
                    log.warn("火山下行错误帧");
                    return null;
                default:
                    log.info("火山下行 非 FULL_RESPONSE, messageType={}", messageType);
                    return null;
            }

            if (compression == GZIP_COMPRESS) {
                payload = gzipDecompress(payload);
            }
            if (payload == null || payload.length == 0) return null;

            String payloadStr = new String(payload, java.nio.charset.StandardCharsets.UTF_8).trim();
            if (payloadStr.isEmpty() || !payloadStr.startsWith("{")) {
                log.info("火山下行 非JSON, payloadLen={}, 前80字符=[{}]", payloadStr.length(), payloadStr.length() > 0 ? payloadStr.substring(0, Math.min(80, payloadStr.length())) : "");
                return null;
            }

            JsonNode root = new ObjectMapper().readTree(payloadStr);
            JsonNode result = root.path("result");
            if (result.isMissingNode()) {
                log.info("火山下行 JSON 无 result, 片段=[{}]", payloadStr.length() > 200 ? payloadStr.substring(0, 200) + "..." : payloadStr);
                return null;
            }

            JsonNode textNode = result.path("text");
            String text = textNode.asText(null);
            if (text != null && !text.isBlank()) return text;

            JsonNode additions = result.path("additions");
            if (!additions.isMissingNode()) {
                if (additions.isArray()) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonNode item : additions) {
                        String t = item.path("text").asText(null);
                        if (t != null && !t.isBlank()) sb.append(t);
                    }
                    text = sb.toString();
                } else {
                    text = additions.path("text").asText(null);
                }
                if (text != null && !text.isBlank()) return text;
            }
            if (result.isArray() && result.size() > 0) {
                text = result.get(0).path("text").asText(null);
                if (text != null && !text.isBlank()) return text;
            }
            // additions 仅有 log_id 等元数据、无 text 时属正常（如无语音或结束确认），仅打 DEBUG
            JsonNode addNode = result.path("additions");
            if (!addNode.isMissingNode() && addNode.isObject() && addNode.size() <= 2 && addNode.has("log_id")) {
                log.debug("火山下行 result 仅含 log_id 等元数据，无转写文本");
                return null;
            }
            java.util.List<String> keys = new java.util.ArrayList<>();
            result.fieldNames().forEachRemaining(keys::add);
            log.info("火山下行 result 无有效 text/additions, result.keys={}, additions片段={}", keys,
                    addNode.isMissingNode() ? "无" : addNode.toString().length() > 150 ? addNode.toString().substring(0, 150) + "..." : addNode.toString());
            return null;
        }
    }
}
