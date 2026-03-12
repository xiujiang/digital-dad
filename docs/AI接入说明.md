# AI 接入说明

## 一、已接入能力

### 1. DeepSeek 大语言模型（对话）

- **用途**：用户提交消息后，生成 AI 回复
- **接口**：`POST /api/c/sessions/{sessionId}/submit`
- **实现**：`DeepSeekAiChatService`
- **模型**：`deepseek-chat`（默认）
- **上下文**：提交时会传入完整对话历史，供 AI 理解上下文

**配置**（`application.yml` 或环境变量）：
```yaml
app:
  ai:
    enabled: true   # 设为 false 则使用占位实现
  deepseek:
    api-key: ${DEEPSEEK_API_KEY:sk-xxx}
    base-url: https://api.deepseek.com
    model: deepseek-chat
```

### 2. 火山引擎流式语音识别

- **用途**：用户发送语音时，实时转写为文字并回传前端
- **接口**：WebSocket `ws://host/api/c/ws/speech-recognition`
- **实现**：`SpeechRecognitionService` + `SpeechRecognitionWebSocketHandler`

**协议说明**：
- 建连后服务端自动发送 full client request 初始化
- 客户端发送 **Binary**：PCM 音频（16kHz, 16bit, 单声道）
- 客户端发送 **Text** `"end"` 或 `"stop"`：表示录音结束
- 服务端返回 **Text**：JSON `{"type":"transcript","text":"转写结果"}` 或 `{"type":"error","message":"错误信息"}`

**前端对接要点**：
- 录音格式：PCM 16k 16bit mono，或使用兼容格式（如 wav 的 PCM 流）
- 建议按约 100～200ms 一包发送
- 结束时发送 `"end"` 或 `"stop"` 文本消息

**配置**：
```yaml
app:
  volcengine:
    app-id: ${VOLCENGINE_APP_ID:7567124792}
    access-token: ${VOLCENGINE_ACCESS_TOKEN:xxx}
    resource-id: volc.seedasr.sauc.duration
    ws-url: wss://openspeech.bytedance.com/api/v3/sauc/bigmodel_async
```

## 二、调用流程

### 对话流程
1. 用户发送消息：`POST /api/c/sessions/{sessionId}/messages`（可多次）
2. 用户确认提交：`POST /api/c/sessions/{sessionId}/submit`
3. 后端调用 DeepSeek 生成回复，并写入消息表
4. 响应中返回 `newMessages`（含 AI 回复）

### 语音转写流程
1. 客户端建立 WebSocket：`/api/c/ws/speech-recognition`，需携带 JWT（Authorization header）
2. 开始录音，按包发送 PCM 二进制
3. 实时接收 `{"type":"transcript","text":"..."}` 并展示
4. 录音结束时发送 `"end"` 文本消息

## 三、安全与凭证

- **勿将 API Key、Token 等写入 Git**
- 生产环境建议通过 `DEEPSEEK_API_KEY`、`VOLCENGINE_APP_ID` 等环境变量注入
- WebSocket 端点需登录后访问，走与 `/api/c/**` 相同的鉴权
