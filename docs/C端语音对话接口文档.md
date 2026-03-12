# C 端语音对话接口文档

本文档描述 C 端与后端的语音对话全流程，以及 C 端需要实现的处理逻辑。

---

## 一、整体流程概览

语音对话分为两条链路，两者串联完成「边说边转写 → 发消息 → AI 回复」：

| 阶段 | 链路 | 说明 |
|------|------|------|
| 1. 实时转写 | WebSocket | 用户录音时，实时把语音转成文字并回传 |
| 2. 发送消息 | HTTP | 用户确认后，将转写结果写入会话消息 |
| 3. AI 回复 | HTTP | 用户提交后，后端调用大模型生成回复 |

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│  C 端                                                                           │
├─────────────────────────────────────────────────────────────────────────────────┤
│  1. 录音 → 2. 音频处理 → 3. WebSocket 发送 → 4. 接收转写结果                      │
│  5. 调用 POST /messages（带 transcriptText）                                     │
│  6. 用户点击提交 → 7. 调用 POST /submit → 8. 接收 AI 回复                         │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、C 端必须处理的内容

### 2.1 音频采集与格式转换

后端语音识别要求：**PCM、16kHz、16bit、单声道**。C 端需在发送前完成转换。

| 处理项 | 说明 | 示例 |
|--------|------|------|
| 重采样 | 将设备采样率转为 16kHz | 44.1kHz / 48kHz → 16kHz |
| 声道转换 | 立体声转单声道 | 左右声道取平均或取其一 |
| 位深转换 | 转为 16bit 有符号整数 | Float32 → Int16（PCM） |
| 格式 | 输出原始 PCM，不压缩 | 不用 mp3、aac 等 |

**各平台常见采集格式：**

| 平台 | 典型采集格式 | C 端需做 |
|------|--------------|----------|
| 浏览器 Web Audio API | 44.1kHz / 48kHz, Float32, 立体声 | 重采样 + 转 mono + Float→Int16 |
| 微信小程序 RecorderManager | mp3 / aac / PCM（可配置） | 若为 PCM 需校验参数；若为压缩格式需解码 |
| 原生 App | 视平台和库而定 | 按实际格式转换至 16k/16bit/mono |

**PCM 16bit 说明**：每个采样点为 2 字节，小端序（Little Endian），有符号整数（-32768～32767）。

---

### 2.2 分包发送策略

- 建议每包时长：**100～200ms**
- 16kHz × 16bit × 1 通道：每 100ms ≈ 3200 字节
- 录音过程中按固定时长或固定字节数分包，通过 WebSocket 以 Binary 形式发送

---

### 2.3 录音开始与结束

| 动作 | C 端行为 |
|------|----------|
| 开始录音 | 建立 WebSocket 连接成功后，开始采集并发送 PCM 包 |
| 结束录音 | 发送文本消息 `"end"` 或 `"stop"`，关闭 WebSocket（或保留连接用于下次录音） |

---

## 三、接口说明

### 3.1 实时语音转写（WebSocket）

**连接地址**：`ws://{host}/api/c/ws/speech-recognition`  
**生产环境**：`wss://{host}/api/c/ws/speech-recognition`

**鉴权**：在 WebSocket 握手请求中携带 JWT：

```
Authorization: Bearer <access_token>
```

> 部分环境的 WebSocket 不支持自定义 Header，可在 URL 传参：`?token=<access_token>`（若后端已支持）

---

**客户端 → 服务端：**

| 消息类型 | 说明 | 示例 |
|----------|------|------|
| Binary | PCM 音频包，16k/16bit/mono | 每包约 3200 字节（100ms） |
| Text | 结束本次录音 | 内容为 `"end"` 或 `"stop"` |

---

**服务端 → 客户端：**

| 消息类型 | 说明 | 格式 |
|----------|------|------|
| Text | 转写结果（增量或完整） | `{"type":"transcript","text":"转写内容"}` |
| Text | 错误信息 | `{"type":"error","message":"错误描述"}` |

**转写结果示例：**

```json
{"type":"transcript","text":"我的童年是在农村度过的"}
```

**错误示例：**

```json
{"type":"error","message":"连接已断开"}
```

---

### 3.2 发送消息（HTTP）

将转写得到的文字写入会话。

**POST** `/api/c/sessions/{sessionId}/messages`

**请求体：**

```json
{
  "content": "我的童年是在农村度过的",
  "audioUrl": "https://xxx/audio.mp3",
  "transcriptText": "我的童年是在农村度过的"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| content | string | 否 | 文本内容，最长 5000 字。有 content 时优先用 content |
| transcriptText | string | 否 | 语音转写文本，无 content 时使用 |
| audioUrl | string | 否 | 语音文件 URL（可选，用于存证） |

**语音场景建议**：`content` 与 `transcriptText` 传同一转写结果即可；如需存证，可上传音频后再传 `audioUrl`。

**响应**：`Result<MessageResponse>`

---

### 3.3 提交给 AI（HTTP）

**POST** `/api/c/sessions/{sessionId}/submit`

**说明**：将当前未提交的消息一并提交，触发 AI 生成回复。

**响应**：`Result<SubmitResultResponse>`

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "newBatchNo": 1,
    "roundCount": 1,
    "newMessages": [
      {
        "id": 2,
        "sessionId": 1,
        "senderType": "AI",
        "messageType": "TEXT",
        "content": "感谢你的分享，能再具体说说...",
        "sequenceNo": 2,
        "batchNo": 1,
        "isSubmitted": true,
        "createdAt": "2025-03-01T10:02:00"
      }
    ]
  }
}
```

`newMessages` 即为本轮的 AI 回复，需在 UI 中展示。

---

### 3.4 其他相关接口

| 接口 | 说明 |
|------|------|
| POST `/api/c/sessions` | 创建/恢复会话，获取 `sessionId` |
| GET `/api/c/sessions/{sessionId}` | 获取会话详情 |
| GET `/api/c/sessions/{sessionId}/messages` | 获取消息列表 |
| PUT `/api/c/sessions/{sessionId}/messages/{messageId}` | 修改未提交消息 |
| DELETE `/api/c/sessions/{sessionId}/messages/{messageId}` | 删除未提交消息 |

---

## 四、完整交互时序

```
用户操作          C 端                    后端                    语音大模型
   │                │                      │                         │
   │  点击开始录音   │                      │                         │
   │───────────────>│  建立 WebSocket       │                         │
   │                │─────────────────────>│  建立连接                │
   │                │                      │────────────────────────>│
   │                │                      │                         │
   │  持续说话      │  采集 + 格式转换       │                         │
   │                │  分包(100~200ms)      │                         │
   │                │─────────────────────>│  转发 PCM                │
   │                │                      │────────────────────────>│
   │                │                      │<────────────────────────│  识别结果
   │                │<─────────────────────│  转发 JSON               │
   │<───────────────│  展示转写文字         │                         │
   │                │                      │                         │
   │  点击结束      │  发送 "end"           │                         │
   │                │─────────────────────>│  发送负包                │
   │                │                      │────────────────────────>│
   │                │                      │                         │
   │  确认发送      │  POST /messages       │                         │
   │                │  {transcriptText}    │                         │
   │                │─────────────────────>│  落库                    │
   │                │                      │                         │
   │  点击提交      │  POST /submit         │                         │
   │                │─────────────────────>│  调用 DeepSeek           │
   │                │                      │────────────────────────>│ 大模型
   │                │                      │<────────────────────────│ AI 回复
   │                │<─────────────────────│  newMessages             │
   │<───────────────│  展示 AI 回复         │                         │
```

---

## 五、C 端开发检查清单

### 5.1 音频处理

- [ ] 实现 16kHz 重采样（若采集非 16k）
- [ ] 实现立体声转单声道
- [ ] 实现 Float32/其他格式 → Int16 PCM
- [ ] 校验输出：PCM、16kHz、16bit、mono

### 5.2 WebSocket

- [ ] 建连时携带 JWT 鉴权
- [ ] 按约 100～200ms 分包发送 Binary
- [ ] 监听并解析 `{"type":"transcript","text":"..."}` 展示转写
- [ ] 处理 `{"type":"error",...}` 错误
- [ ] 结束时发送 `"end"` 或 `"stop"`

### 5.3 消息与 AI

- [ ] 转写完成后调用 POST `/messages`，传入 `content` 或 `transcriptText`
- [ ] 用户确认后调用 POST `/submit`，接收并展示 `newMessages`
- [ ] 支持未提交消息的修改、删除（PUT/DELETE）

### 5.4 异常与体验

- [ ] WebSocket 断线重连
- [ ] 网络异常提示
- [ ] 录音权限申请与失败提示

---

## 六、常见问题

**Q：小程序可以直连这个 WebSocket 吗？**  
A：可以。需在微信后台配置合法域名（含 wss），并确保录音格式经处理后符合 16k/16bit/mono PCM。

**Q：必须用 PCM 吗？能否上传 mp3 文件再转写？**  
A：当前实时转写 WebSocket 仅支持 PCM 流。若采用「录完再上传」方式，需另行实现录音文件识别接口（未在本文档范围）。

**Q：转写结果是增量还是整句？**  
A：火山引擎为流式识别，可能多次返回，每次可能是增量或整句，C 端可累积或按最新结果展示，视产品需求而定。

**Q：同一 WebSocket 可多次录音吗？**  
A：理论上可复用，但每次录音结束需发送 `"end"`，下一段录音需重新发 full client request。当前实现为每次建连即发一次 full request，建议每次录音新建连接更简单。
