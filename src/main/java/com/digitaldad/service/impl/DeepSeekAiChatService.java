package com.digitaldad.service.impl;

import com.digitaldad.common.config.DeepSeekProperties;
import com.digitaldad.dto.ChatMessage;
import com.digitaldad.service.AiChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * DeepSeek 大模型 AI 对话实现
 * <p>当 app.ai.enabled=true 时启用，通过 DeepSeek API 完成对话、小结、故事、交付物生成。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class DeepSeekAiChatService implements AiChatService {

    private static final okhttp3.MediaType JSON = okhttp3.MediaType.get("application/json; charset=utf-8");

    private final DeepSeekProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    /**
     * 将用户消息列表转换为 ChatMessage 后调用带历史版本的对话
     */
    @Override
    public String chat(String systemPrompt, List<String> userMessages) {
        List<ChatMessage> messages = new java.util.ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(ChatMessage.system(systemPrompt));
        }
        for (String u : userMessages) {
            if (u != null && !u.isBlank()) {
                messages.add(ChatMessage.user(u));
            }
        }
        return chatWithHistory(systemPrompt, messages);
    }

    /**
     * 调用 DeepSeek Chat Completions API 进行对话
     */
    @Override
    public String chatWithHistory(String systemPrompt, List<ChatMessage> messages) {
        List<Map<String, String>> apiMessages = new java.util.ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            apiMessages.add(Map.of("role", "system", "content", systemPrompt));
        }
        for (ChatMessage m : messages) {
            if (m != null && m.getContent() != null && !m.getContent().isBlank()) {
                apiMessages.add(Map.of("role", m.getRole().toLowerCase(), "content", m.getContent()));
            }
        }
        if (apiMessages.isEmpty()) {
            return "请继续说，我在听。";
        }

        Map<String, Object> request = new HashMap<>();
        request.put("model", properties.getModel());
        request.put("messages", apiMessages);
        request.put("stream", false);

        String url = properties.getBaseUrl().replaceAll("/$", "") + "/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + properties.getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    JsonNode msg = choices.get(0).path("message");
                    JsonNode content = msg.path("content");
                    return content.asText("");
                }
            }
        } catch (Exception e) {
            log.error("DeepSeek API 调用失败", e);
            return "抱歉，我暂时无法回复，请稍后再试。";
        }
        return "抱歉，我暂时无法回复，请稍后再试。";
    }

    /**
     * 流式调用 DeepSeek Chat Completions API，每收到 delta 即回调 onDelta
     */
    @Override
    public void chatWithHistoryStream(String systemPrompt, List<ChatMessage> messages, Consumer<String> onDelta) {
        List<Map<String, String>> apiMessages = new java.util.ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            apiMessages.add(Map.of("role", "system", "content", systemPrompt));
        }
        for (ChatMessage m : messages) {
            if (m != null && m.getContent() != null && !m.getContent().isBlank()) {
                apiMessages.add(Map.of("role", m.getRole().toLowerCase(), "content", m.getContent()));
            }
        }
        if (apiMessages.isEmpty()) {
            if (onDelta != null) onDelta.accept("请继续说，我在听。");
            return;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("model", properties.getModel());
        request.put("messages", apiMessages);
        request.put("stream", true);

        String url = properties.getBaseUrl().replaceAll("/$", "") + "/v1/chat/completions";
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            log.error("序列化请求体失败", e);
            if (onDelta != null) onDelta.accept("抱歉，我暂时无法回复，请稍后再试。");
            return;
        }

        Request okRequest = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + properties.getApiKey())
                .post(RequestBody.create(jsonBody, JSON))
                .build();

        try (Response response = okHttpClient.newCall(okRequest).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.warn("DeepSeek 流式 API 响应异常: {}", response.code());
                if (onDelta != null) onDelta.accept("抱歉，我暂时无法回复，请稍后再试。");
                return;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;
                    if (data.isEmpty()) continue;
                    try {
                        JsonNode root = objectMapper.readTree(data);
                        JsonNode choices = root.path("choices");
                        if (choices.isArray() && choices.size() > 0) {
                            JsonNode delta = choices.get(0).path("delta");
                            JsonNode content = delta.path("content");
                            if (!content.isMissingNode() && content.isTextual()) {
                                String text = content.asText("");
                                if (text != null && !text.isEmpty() && onDelta != null) {
                                    onDelta.accept(text);
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 忽略单行解析失败（如 finish_reason 等）
                    }
                }
            }
        } catch (IOException e) {
            log.error("DeepSeek 流式 API 调用失败", e);
            if (onDelta != null) onDelta.accept("抱歉，我暂时无法回复，请稍后再试。");
        }
    }

    /**
     * 使用对话 API 生成板块小结（完整打印请求与响应，不截断）
     */
    @Override
    public String generateSummary(String prompt, String conversation, String boardName) {
        String userContent = "请根据以下采访对话生成板块小结，板块名称：「" + boardName + "」。\n\n对话内容：\n" + conversation;
        log.info("[小结-大模型请求] system 全文:\n{}", prompt != null ? prompt : "null");
        log.info("[小结-大模型请求] user 全文:\n{}", userContent);
        String result = chat(prompt, List.of(userContent));
        log.info("[小结-大模型响应] 完整内容:\n{}", result != null ? result : "null");
        return result;
    }

    /**
     * 使用对话 API 生成故事叙述
     */
    @Override
    public String generateStory(String prompt, String conversation, String boardName) {
        String userContent = "请将以下小结整理成一段温暖的故事叙述。板块：「" + boardName + "」。\n\n小结内容：\n" + conversation;
        return chat(prompt, List.of(userContent));
    }

    /**
     * 使用对话 API 生成交付物
     */
    @Override
    public String generateDeliverable(String prompt, String materialsJson, String contentType) {
        String userContent = "请根据以下已确认的素材 JSON 生成「" + contentType + "」类型的交付物。\n\n素材：\n" + (materialsJson != null ? materialsJson : "{}");
        return chat(prompt, List.of(userContent));
    }
}
