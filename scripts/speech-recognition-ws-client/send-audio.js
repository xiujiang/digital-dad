#!/usr/bin/env node
/**
 * 将本地音频文件按「实时流」方式发送到语音识别 WebSocket 接口，用于 Postman/接口自测。
 *
 * 用法:
 *   node send-audio.js <音频文件> [--url=ws://localhost:8080] [--token=JWT]
 * 或设置环境变量:
 *   TOKEN=your_jwt node send-audio.js ./demo.wav
 *
 * 音频要求: PCM 16kHz 16bit 单声道。支持:
 *   - .pcm / .raw: 纯 PCM 数据
 *   - .wav: 自动跳过 44 字节 WAV 头后按 PCM 发送
 *
 * 发包: 每包 6400 字节(约 200ms)，间隔 150ms，模拟实时。
 */

const WebSocket = require('ws');
const fs = require('fs');
const path = require('path');

const CHUNK_BYTES = 6400;   // 200ms @ 16k 16bit mono
const CHUNK_INTERVAL_MS = 150;

function parseArgs() {
  const args = process.argv.slice(2);
  let filePath = null;
  let url = process.env.WS_URL || 'ws://localhost:8080';
  let token = process.env.TOKEN || '';

  for (const a of args) {
    if (a === '--help' || a === '-h') {
      console.log(`
用法: node send-audio.js <音频文件> [选项]
  音频文件  必填。.pcm/.raw 为纯 PCM；.wav 会跳过 44 字节头。
选项:
  --url=WS_URL   WebSocket 根地址，默认 ws://localhost:8080
  --token=JWT    登录后获得的 JWT，也可用环境变量 TOKEN

示例:
  TOKEN=eyJ... node send-audio.js ./test.wav
  node send-audio.js ./record.pcm --url=ws://101.34.64.224:8080 --token=eyJ...
`);
      process.exit(0);
    }
    if (a.startsWith('--url=')) url = a.slice(6);
    else if (a.startsWith('--token=')) token = a.slice(8);
    else if (!a.startsWith('--')) filePath = a;
  }

  if (!filePath) {
    console.error('请提供音频文件路径。用法: node send-audio.js <文件> [--url=...] [--token=...]');
    process.exit(1);
  }
  if (!token) {
    console.error('请提供 token：--token=JWT 或环境变量 TOKEN');
    process.exit(1);
  }

  const base = url.replace(/\/$/, '');
  const wsUrl = `${base}/api/c/ws/speech-recognition?token=${encodeURIComponent(token)}`;
  return { filePath, wsUrl };
}

function readPcmFromFile(filePath) {
  const buf = fs.readFileSync(filePath);
  const ext = path.extname(filePath).toLowerCase();
  if (ext === '.wav') {
    // 标准 PCM WAV 头 44 字节
    if (buf.length <= 44) throw new Error('WAV 文件过短');
    return Buffer.from(buf.subarray(44));
  }
  return buf;
}

function main() {
  const { filePath, wsUrl } = parseArgs();
  const pcm = readPcmFromFile(filePath);
  console.log('音频长度:', pcm.length, '字节 (约', (pcm.length / 32000).toFixed(2), '秒)');
  console.log('连接:', wsUrl.replace(/\?token=[^&]+/, '?token=***'));

  const ws = new WebSocket(wsUrl);

  ws.on('open', () => {
    console.log('已连接，开始按实时节奏发送音频...');
    let offset = 0;
    const sendNext = () => {
      if (offset >= pcm.length) {
        ws.send('end');
        console.log('已发送 end，等待最终结果');
        return;
      }
      const chunk = pcm.subarray(offset, Math.min(offset + CHUNK_BYTES, pcm.length));
      ws.send(chunk);
      offset += chunk.length;
      if (offset < pcm.length) {
        setTimeout(sendNext, CHUNK_INTERVAL_MS);
      } else {
        setTimeout(() => { ws.send('end'); console.log('已发送 end'); }, CHUNK_INTERVAL_MS);
      }
    };
    sendNext();
  });

  ws.on('message', (data) => {
    const raw = typeof data === 'string' ? data : data.toString('utf8');
    console.log('收到:', raw);
    try {
      const o = JSON.parse(raw);
      if (o.type === 'transcript' && o.text) console.log('>>> 转写:', o.text);
      if (o.type === 'error') console.error('>>> 错误:', o.message);
    } catch (_) {}
  });

  ws.on('close', (code, reason) => {
    console.log('连接关闭:', code, reason?.toString() || '');
  });

  ws.on('error', (err) => {
    console.error('WebSocket 错误:', err.message);
  });
}

main();
