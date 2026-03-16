# C 端用户在当前项目下的流程与状态分析

本文档说明：登录后，前端如何根据「当前用户 + 当前项目」判断用户处于哪一步，应展示什么页面。

---

## 一、整体流程概览（用户从分享链接到完成采访）

```
分享链接(entry) → 获取项目信息(免登录)
       ↓
   用户登录(微信)
       ↓
 获取「当前用户在该项目下的状态」  ← 待实现接口
       ↓
┌──────────────────────────────────────────────────────────────────┐
│ 1. 未绑定 → 选择身份(新郎/新娘) → POST bind → 得到 projectId、role   │
└──────────────────────────────────────────────────────────────────┘
       ↓
┌──────────────────────────────────────────────────────────────────┐
│ 2. 已绑定、无进行中会话 → POST createOrResume(projectId)          │
│    → 得到 sessionId，进入采访                                     │
└──────────────────────────────────────────────────────────────────┘
       ↓
┌──────────────────────────────────────────────────────────────────┐
│ 3. 会话中：按「板块」顺序进行，每个板块内循环：                     │
│    发消息 → 提交(submit) → [可选]生成小结(createSummary)           │
│    → 确认小结(confirmSummary) → 进入下一板块 或 全部完成           │
└──────────────────────────────────────────────────────────────────┘
       ↓
  全部板块确认完 → 会话 COMPLETED，参与者 COMPLETED → 结束
```

---

## 二、用户应获取的「状态」清单（供前端分支判断）

前端需要根据以下状态决定：是展示「选身份」「进入采访」「对话页」「小结确认页」还是「已完成」。

| 维度 | 字段/含义 | 用途 |
|------|-----------|------|
| **是否已绑定** | `bound: boolean` | 未绑定 → 必须走「选择身份」+ bind |
| **参与者信息** | `role`（已绑定时有） | C 端不暴露参与者 ID；role 用于展示（新郎/新娘），后续接口用 projectId |
| **是否有进行中会话** | 有则返回 `sessionId` | 无 → 需调 createOrResume 再进采访 |
| **会话状态** | `sessionStatus` | ACTIVE / WAITING_CONFIRM / READY / COMPLETED / INTERRUPTED，决定是否还能发消息、是否在等确认小结、是否已结束 |
| **当前板块** | `currentProjectBoardId`, `boardCode`, `boardName`, `currentBoardOrder` | 当前在第几块、叫什么，用于展示进度与文案 |
| **板块进度** | `boards[]`（每块 `projectBoardId`, `boardName`, `isCurrent`, `isCompleted`） | 用于进度条/步骤条：哪些已完成、当前在哪一块 |
| **当前板块轮数** | `currentBoardRoundCount`, `maxRoundsPerBoard` | 本板块已用几轮、最多几轮（可提示「还可聊 N 轮」） |
| **是否在「等待确认小结」** | `currentSummaryId`, `currentSummaryStatus`（或布尔 `hasSummaryWaitingConfirm`） | 若当前板块有小结且状态为 WAITING_CONFIRM → 展示小结确认页；否则展示对话/发消息页 |
| **参与者整体状态** | `participantStatus` | ENTERED / IN_PROGRESS / COMPLETED 等，可与 session 一起判断「是否全部完成」 |

---

## 三、建议的「步骤」枚举（前端可据此做路由/展示）

把上述状态聚合成一个「当前步骤」，前端只需根据 step 跳转即可：

| 步骤 step | 含义 | 前端应展示 | 后端状态条件简要 |
|-----------|------|------------|------------------|
| `NOT_BOUND` | 未绑定身份 | 选择身份页（新郎/新娘），调 bind | 当前用户在该项目下无 participant |
| `BOUND_NO_SESSION` | 已绑定，未进会话 | 引导「开始采访」按钮，调 createOrResume(projectId) | 有 participant，无 ACTIVE/WAITING_CONFIRM/READY 的 session |
| `IN_CHAT` | 会话中，对话阶段 | 对话/发消息页，可发消息、提交、或「生成小结」 | 有 session，status=ACTIVE（且当前板块没有 WAITING_CONFIRM 的小结） |
| `WAITING_SUMMARY_CONFIRM` | 当前板块小结待确认 | 小结确认页（勾选条目、确认） | 有 session，当前板块存在小结且 status=WAITING_CONFIRM |
| `ALL_COMPLETED` | 全部完成 | 完成页/感谢页 | session.status=COMPLETED 或 participant.status=COMPLETED |

说明：

- **IN_CHAT** 与 **WAITING_SUMMARY_CONFIRM** 的区别：同一 session、同一板块下，若已调用「生成小结」且小结为 WAITING_CONFIRM，则应为 `WAITING_SUMMARY_CONFIRM`；否则为 `IN_CHAT`（继续聊或提交）。
- **BOUND_NO_SESSION**：可能是首次进入，也可能是之前会话已 COMPLETED/INTERRUPTED，前端可统一视为「需要重新 createOrResume」再进会话（业务若允许多次采访则如此；若只允许一次则需产品约定）。

---

## 四、每个 step 携带的信息（响应里会带上的 ID / 文案）

接口返回的是**同一份响应体**，根据当前状态把 `step` 设为上述枚举，并**按步骤填充下面这些字段**。前端拿到后可直接用对应 ID 调后续接口，无需再请求详情。

| step | 该步骤下会携带的信息 | 前端用法 |
|------|----------------------|----------|
| **NOT_BOUND** | `projectId`（路径或 body 已有）、`bound=false`。无 `sessionId`。 | 用 `projectId` 调 `POST /api/c/projects/{projectId}/bind`，传 `role`。 |
| **BOUND_NO_SESSION** | `bound=true`，`role`，`participantStatus`。无 `sessionId`。 | 用 `projectId` 调 `POST /api/c/sessions`（body 传 `projectId`），得到 `sessionId` 后进采访。 |
| **IN_CHAT** | `sessionId`，`sessionStatus=ACTIVE`，`currentProjectBoardId`，`boardCode`，`boardName`，`currentBoardOrder`，`boards[]`，`currentBoardRoundCount`，`maxRoundsPerBoard`。无待确认小结时 `currentSummaryId` 为 null。 | 用 `sessionId` 调消息列表、发消息、提交、生成小结等（如 `GET .../sessions/{sessionId}/messages`，`POST .../submit`，`POST .../summaries`）。 |
| **WAITING_SUMMARY_CONFIRM** | 同上，且 `currentSummaryId`、`currentSummaryStatus=WAITING_CONFIRM`。仍有 `sessionId`、`boards[]` 等。 | 用 `currentSummaryId` 拉小结详情、改条目、确认（如 `GET .../board-summaries/{currentSummaryId}`，`POST .../confirm`）。会话相关仍用 `sessionId`。 |
| **ALL_COMPLETED** | `role`，`participantStatus=COMPLETED`；可有 `sessionId`、`sessionStatus=COMPLETED`，以及最后一次的 `boards[]`（全部完成）。 | 仅展示完成页；无需再调创建会话或发消息。 |

**小结**：  
- **sessionId**：在 `IN_CHAT`、`WAITING_SUMMARY_CONFIRM`、`ALL_COMPLETED` 都会带（有会话时）。  
- C 端不暴露参与者 ID，前端仅依赖「用户 + 项目」及 step、sessionId、currentSummaryId 等即可。  
- **currentSummaryId**：仅在 `WAITING_SUMMARY_CONFIRM` 时有值，用于小结确认相关接口。

---

## 五、后端已有数据与枚举（便于实现「状态」接口）

### 4.1 参与者 Participant

- **表**：`project_participant`
- **关键字段**：`project_id`, `user_id`, `role_type`(GROOM/BRIDE), `status`, `current_board_order`
- **ParticipantStatus**：`INVITED` | `ENTERED` | `IN_PROGRESS` | `COMPLETED` | `ABANDONED`
  - 绑定后为 `ENTERED`；全部板块确认完后为 `COMPLETED`。

### 4.2 会话 InterviewSession

- **表**：`interview_session`
- **关键字段**：`participant_id`, `current_project_board_id`, `status`, `round_count`
- **SessionStatus**：`READY` | `ACTIVE` | `WAITING_CONFIRM` | `COMPLETED` | `INTERRUPTED`
  - 进行中多为 `ACTIVE`；全部板块确认完为 `COMPLETED`。
  - 「恢复会话」时会把 ACTIVE、WAITING_CONFIRM、READY 都视为可恢复。

### 4.3 小结 BoardSummary

- **表**：`board_summary`
- **关键字段**：`session_id`, `project_board_id`, `status`
- **SummaryStatus**：`DRAFT` | `GENERATED` | `WAITING_CONFIRM` | `CONFIRMED`
  - 用户需要「确认」的是 `WAITING_CONFIRM`；确认后变为 `CONFIRMED`。

### 4.4 板块与轮数

- 项目下有多块 `project_board`，按 `display_order` 顺序进行。
- 每块可进行多轮对话，由 `session_board_rounds` 记录每块已用轮数；上限来自配置（如 `getInterviewMaxRoundsPerBoard()`）。

---

## 六、接口设计建议（供后端实现）

- **路径**：如 `GET /api/c/projects/{projectId}/my-status` 或 `GET /api/c/entry/{token}/my-status`（登录后带 token，用 projectId 或 token 均可）。
- **响应**：建议包含：
  - `bound`, `role`, `participantStatus`
  - `sessionId`, `sessionStatus`（无会话可 null）
  - `currentProjectBoardId`, `boardCode`, `boardName`, `currentBoardOrder`
  - `boards[]`（与现有 SessionResponse.boards 一致）
  - `currentBoardRoundCount`, `maxRoundsPerBoard`
  - `currentSummaryId`, `currentSummaryStatus`（或 `hasSummaryWaitingConfirm`）
  - 可选：聚合出的 `step`（NOT_BOUND | BOUND_NO_SESSION | IN_CHAT | WAITING_SUMMARY_CONFIRM | ALL_COMPLETED），便于前端直接分支。

这样前端在登录后只调这一个接口，即可根据 `step`（或上述各字段）决定进入选身份、开始采访、对话页、小结确认页或完成页。
