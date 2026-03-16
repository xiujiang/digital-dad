# C 端用户完整链路：从扫码到结束

文字描述 + 接口名，按时间顺序。

---

## 一、扫码进入与登录

| 步骤 | 描述 | 接口 |
|------|------|------|
| 1 | 用户打开分享链接/扫码，**免登录**获取项目基本信息（名称、新郎新娘、婚期、主题、可选角色列表） | `GET /api/c/entry/{token}` |
| 2 | 前端引导用户完成微信登录；用户授权后，前端将 openid 及用户信息（昵称、头像等）提交，后端完成登录/注册并返回 token、用户信息 | `POST /api/auth/wechat-login` |
| 3 | 登录后，根据「当前用户 + 当前项目」拉取用户在该项目下的状态（是否已绑定、步骤、sessionId、板块进度、待确认小结等），前端据此决定进入哪一页 | `GET /api/c/projects/{projectId}/my-status` |

---

## 二、选身份（未绑定时）

| 步骤 | 描述 | 接口 |
|------|------|------|
| 4 | 若 my-status 的 step 为 `NOT_BOUND`：展示选择身份页（新郎/新娘），用户选择后提交绑定 | `POST /api/c/projects/{projectId}/bind` |
| — | 绑定成功后得到 projectId、role；前端可再次调 my-status 或直接进入「开始采访」 | （同上或 `GET /api/c/projects/{projectId}/my-status`） |

---

## 三、进入采访（已绑定、无进行中会话时）

| 步骤 | 描述 | 接口 |
|------|------|------|
| 5 | 若 step 为 `BOUND_NO_SESSION`：用户点击「开始采访」，用 projectId 创建或恢复会话（body 传 projectId），得到 sessionId 及当前板块、轮数等信息 | `POST /api/c/sessions` |
| 6 | 可选：需要展示会话详情、板块列表、轮数时，可直接用 my-status 已返回的 session 信息，或再查一次会话详情 | `GET /api/c/sessions/{sessionId}` |

---

## 四、会话中：对话与提交（每个板块内循环）

| 步骤 | 描述 | 接口 |
|------|------|------|
| 7 | 展示当前板块对话页；拉取已有消息列表 | `GET /api/c/sessions/{sessionId}/messages` |
| 8 | 用户发送文本或语音（含转写）消息，可多次 | `POST /api/c/sessions/{sessionId}/messages` |
| 9 | 用户可修改、删除**未提交**的消息 | `PUT /api/c/sessions/{sessionId}/messages/{messageId}`、`DELETE /api/c/sessions/{sessionId}/messages/{messageId}` |
| 10 | 用户点击「提交」：将本批消息提交给 AI，生成回复，本板块轮数 +1 | `POST /api/c/sessions/{sessionId}/submit` |
| 11 | 若需展示语音转写剩余配额（如录音前） | `GET /api/c/speech-quota` |

---

## 五、生成小结（生成即确认，当前板块）

| 步骤 | 描述 | 接口 |
|------|------|------|
| 12 | 用户选择「生成小结」：根据当前板块对话生成小结并**立即确认**（写入素材快照、推进板块进度；若最后一块则会话与参与者标记为已完成） | `POST /api/c/sessions/{sessionId}/summaries` |
| 13 | 可选：展示本板块小结（创建接口已返回小结对象，也可再拉当前小结或详情） | `GET /api/c/sessions/{sessionId}/summaries/current` 或 `GET /api/c/board-summaries/{summaryId}` |

---

## 六、多板块循环与状态刷新

| 步骤 | 描述 | 接口 |
|------|------|------|
| — | 生成小结（即确认）后进入下一板块，前端可再次调 **my-status** 得到新的 step、currentProjectBoardId、boards；若 step 仍为 `IN_CHAT` 则回到「四、对话与提交」继续该板块；若为 `ALL_COMPLETED` 则进「七、结束」 | `GET /api/c/projects/{projectId}/my-status` |

---

## 七、结束

| 步骤 | 描述 | 接口 |
|------|------|------|
| 16 | my-status 的 step 为 `ALL_COMPLETED`：展示完成页/感谢页，流程结束 | 无需再调新接口，仅展示；可选再次 `GET /api/c/projects/{projectId}/my-status` 做刷新 |

---

## 八、可选能力（故事、人物等）

| 描述 | 接口 |
|------|------|
| 创建/获取故事 | `POST /api/c/sessions/{sessionId}/stories`、`GET /api/c/sessions/{sessionId}/stories` |
| 人物列表/新增/修改/删除 | `GET /api/c/sessions/{sessionId}/persons`、`POST /api/c/sessions/{sessionId}/persons`、`PUT /api/c/key-persons/{personId}`、`DELETE /api/c/key-persons/{personId}` |

---

## 九、链路小结（仅列主流程接口名）

1. `GET /api/c/entry/{token}` — 扫码获取项目信息（免登录）  
2. `POST /api/auth/wechat-login` — 微信登录/注册  
3. `GET /api/c/projects/{projectId}/my-status` — 获取当前用户在该项目下的状态  
4. `POST /api/c/projects/{projectId}/bind` — 选身份绑定（step=NOT_BOUND 时）  
5. `POST /api/c/sessions` — 创建/恢复会话，进入采访（step=BOUND_NO_SESSION 时）  
6. `GET /api/c/sessions/{sessionId}/messages` — 获取消息列表  
7. `POST /api/c/sessions/{sessionId}/messages` — 发送消息  
8. `POST /api/c/sessions/{sessionId}/submit` — 提交给 AI  
9. `POST /api/c/sessions/{sessionId}/summaries` — 生成小结（生成即确认，进入下一板块或结束）  
10. `GET /api/c/board-summaries/{summaryId}` — 小结详情（含条目，可选）  
11. 再次 `GET /api/c/projects/{projectId}/my-status` 判断 step，循环 6～9 或进入完成页  

以上即 C 端用户从扫码到结束的完整链路（文字描述 + 接口名）。
