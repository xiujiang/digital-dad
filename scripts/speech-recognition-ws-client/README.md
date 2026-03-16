# 语音识别 WebSocket 用音频文件自测

把**本地音频文件**按「实时流」发送到 ` /api/c/ws/speech-recognition`，用于不依赖前端的接口联调。

## 准备

- Node.js 14+
- 音频：**PCM 16kHz 16bit 单声道**
  - `.pcm` / `.raw`：纯 PCM
  - `.wav`：脚本会跳过 44 字节 WAV 头再发送

若只有 MP3/其他格式，可先用 ffmpeg 转成 16k 16bit 单声道 PCM 或 WAV：

```bash
ffmpeg -i input.mp3 -ar 16000 -ac 1 -f s16le output.pcm
# 或
ffmpeg -i input.mp3 -ar 16000 -ac 1 output.wav
```

## 安装依赖

```bash
cd scripts/speech-recognition-ws-client
npm install
```

## 运行

先在后端或 Postman 里用「主持人登录」或「微信小程序登录」拿到 `token`，再执行：

```bash
# 必填：音频文件、token（参数或环境变量）
node send-audio.js ./你的音频.wav --token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# 或使用环境变量
TOKEN=eyJ... node send-audio.js ./demo.pcm

# 指定服务地址（默认 ws://localhost:8080）
node send-audio.js ./demo.wav --url=ws://101.34.64.224:8080 --token=...
```

控制台会打印服务端返回的 `{"type":"transcript","text":"..."}` 和 `{"type":"error","message":"..."}`。

## Postman 配合

- Postman 里「实时语音转写(WebSocket)」请求的 URL 为：  
  `{{baseUrlWs}}/api/c/ws/speech-recognition?token={{token}}`
- 用本脚本可**不手工发 Binary**：脚本从文件按 200ms/包、150ms 间隔发送 PCM，并自动发 `end`，等同于前端实时录音再结束。

变量 `baseUrlWs`、`token` 与 Postman 中一致即可（如本地 `ws://localhost:8080`，远程 `ws://101.34.64.224:8080`）。
