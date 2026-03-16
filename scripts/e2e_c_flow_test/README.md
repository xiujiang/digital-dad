# C 端「从头到生成故事」E2E 测试说明

## 测试目标

验证「会话按板块」设计下，从 C 端进入 → 绑定 → 多板块对话 → 创建小结（生成即确认）→ 生成故事 的完整流程是否串通。

## 应覆盖的测试内容

| 序号 | 步骤 | 接口 | 验证点 |
|-----|------|------|--------|
| 1 | 登录拿 token | POST /api/auth/login-password | 获得 C 端可用 token（脚本用主持人账号模拟参与者） |
| 2 | 获取当前状态与板块 | GET /api/c/projects/{projectId}/my-status | step、currentProjectBoardId、boards[].projectBoardId 存在且一致 |
| 3 | 进入某板块（创建/恢复会话） | POST /api/c/sessions | body 含 projectId + projectBoardId（+ 未绑定时 role）；返回 sessionId，currentProjectBoardId 等于请求的 projectBoardId |
| 4 | 发送消息 | POST /api/c/sessions/{sessionId}/messages | 消息落库，lastActiveAt 更新 |
| 5 | 提交会话（触发 AI 回复） | POST /api/c/sessions/{sessionId}/submit | 返回 newMessages，轮数+1 |
| 6 | 创建小结（生成即确认） | POST /api/c/sessions/{sessionId}/summaries | 返回小结对象并完成确认（写入素材快照、推进板块进度） |
| 7 | 再次 my-status（下一板块） | GET /api/c/projects/{projectId}/my-status | currentProjectBoardId 变为下一板块；若还有未完成板块则 sessionId 可能为 null |
| 8 | 进入下一板块会话 | POST /api/c/sessions | 用新的 projectBoardId 得到新 sessionId，重复 4–6 |
| 9 | 创建故事 | POST /api/c/sessions/{sessionId}/stories | 基于当前板块会话生成故事，返回故事对象 |
| 10 | 获取故事 | GET /api/c/sessions/{sessionId}/stories?projectBoardId=xxx | 能按会话+板块查到已生成的故事 |

## 运行方式

见同目录下 `run_e2e.py`。需先启动后端，并配置 `BASE_URL`、`PHONE`、`PASSWORD`、`PROJECT_ID`（可选 `MAX_BOARDS` 限制测试板块数）。

**测试时每板块 AI 对话轮数改为 2：** 启动后端时加 JVM 参数或环境变量，例如：
- `mvn spring-boot:run -Dinterview.max_rounds_per_board=2`
- 或 `export INTERVIEW_MAX_ROUNDS_PER_BOARD=2` 后再启动

脚本会打印每一次接口调用的完整响应 `data`，便于核对结果。
