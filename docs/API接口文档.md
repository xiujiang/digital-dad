# 数字爸爸 API 接口文档

数字爸爸 - 婚礼采访与生成平台接口说明。

---

## 使用说明与路径约定

**调用流程：**
1. **主持人/超管**：调用「认证 - 发送验证码」获取验证码，再调用「认证 - 登录」（或「密码登录」），通过请求体 `admin` 区分身份，获取 token。
2. **C 端（微信小程序）**：前端调用 `wx.login()` 获得临时 **code**，将 code（及可选昵称、头像等）调用「认证 - 微信小程序登录」；后端用 code 调微信 jscode2session 换 openid，查/建用户、写登录流水并签发 token。
3. 需认证的接口在请求头中携带：`Authorization: Bearer <token>`。

**基础地址：** `{{baseUrl}}`（示例：`http://101.34.64.224:8080`）

**路径与角色约定（合并后）：**

| 路径前缀 | 说明 | 鉴权 |
|----------|------|------|
| `/api/auth` | 认证（登录、/me、退出、发码） | 登录/发码免认证；/me、logout 需已登录 |
| `/api/c/*` | C 端（参与者：绑定、会话、消息、小结、故事、人物） | 需登录（C 端身份） |
| `/api/projects` | 项目及项目下板块、交付物 | 主持人 + 超管（HOST 仅本人，SUPER_ADMIN 全部/任意） |
| `/api/board-meta` | 板块元数据 | 主持人 + 超管（列表：HOST 仅启用，SUPER_ADMIN 全部；增删改仅超管） |
| `/api/deliverable-meta` | 交付物元数据 | 主持人 + 超管（列表：HOST 仅启用，SUPER_ADMIN 全部；增删改仅超管） |
| `/api/users` | 当前用户资料、用户/主持人管理 | /me 共用；列表、详情、状态、会员、配额等仅超管 |
| `/api/deliverables` | 交付物按 ID 查/改/删 | 主持人 + 超管（项目归属或超管） |
| `/api/admin/*` | 提示词、语音配额等仅超管能力 | 仅超管 |

**通用响应结构（所有接口均包装为此格式）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| code | number | 业务状态码，成功为 200；失败为 4xx/5xx |
| message | string | 提示信息，成功一般为 "success"，失败为错误描述 |
| data | object / array / null | 业务数据，失败时可为 null；部分接口无返回体时也为 null |

**分页响应（列表类接口的 data 为分页对象时）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| content | array | 当前页的数据列表 |
| totalElements | number | 总记录数（跨所有页） |
| totalPages | number | 总页数 |
| size | number | 当前页每页条数 |
| number | number | 当前页码（从 0 开始，即第 1 页为 0） |
| first | boolean | 是否为第一页 |
| last | boolean | 是否为最后一页 |
| numberOfElements | number | 当前页实际条数 |
| empty | boolean | 当前页是否无数据 |

---

## 一、公共

### 1.1 根路径 - 服务状态

**接口含义：** 检查服务是否存活，根路径可访问即表示服务正常。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/` |
| 认证 | 否 |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体：** 无  

**响应：** 无统一 JSON 结构，通常为 200 或简单文案。

---

### 1.2 健康检查

**接口含义：** 提供标准健康检查端点，用于负载均衡或监控探活。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/health` |
| 认证 | 否 |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体：** 无  

**响应：** 由服务端定义，通常为 200 表示健康。

---

## 二、认证

### 2.1 发送验证码

**接口含义：** 向指定手机号发送短信验证码，用于登录前获取验证码。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/auth/send-code` |
| 认证 | 否 |
| Content-Type | `application/json` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| phone | string | 是 | 手机号，需符合中国大陆 11 位、1 开头（如 1[3-9] 开头） |

**请求示例：**
```json
{
  "phone": "13800138000"
}
```

**响应体：** 成功时 `code` 为 200，`data` 为 null，表示验证码已发送。

---

### 2.2 登录（主持人 / 超管共用）

**接口含义：** 使用手机号 + 验证码登录，返回访问令牌（token）。通过请求体中的 `admin` 区分身份：不传或 `false` 为主持人登录（未注册则自动注册并赋 HOST）；`true` 为超管登录（须具备 SUPER_ADMIN 角色）。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/auth/login` |
| 认证 | 否 |
| Content-Type | `application/json` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| phone | string | 是 | 手机号，格式同发送验证码 |
| code | string | 是 | 6 位数字验证码 |
| admin | boolean | 否 | 默认 false。true 表示超管登录，该手机号须已具备 SUPER_ADMIN 角色 |

**请求示例（主持人）：**
```json
{
  "phone": "13800138000",
  "code": "123456"
}
```

**请求示例（超管）：**
```json
{
  "phone": "13800138000",
  "code": "123456",
  "admin": true
}
```

**响应体（data 为登录结果对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| token | string | JWT 访问令牌，后续请求需在 Header 中携带 `Authorization: Bearer <token>` |
| userId | number | 当前登录用户的用户 ID |
| userType | string | 主角色，如 HOST、SUPER_ADMIN |
| roles | string[] | 角色列表 |
| name | string | 用户姓名/昵称 |
| phone | string | 用户手机号 |

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "userType": "HOST",
    "roles": ["HOST"],
    "name": "主持人昵称",
    "phone": "13800138000"
  }
}
```

---

### 2.3 密码登录（主持人 / 超管）

**接口含义：** 使用手机号 + 密码登录。须已通过验证码登录并设置过密码；超管须具备 SUPER_ADMIN 角色。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/auth/login-password` |
| 认证 | 否 |
| Content-Type | `application/json` |

**请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| phone | string | 是 | 手机号 |
| password | string | 是 | 密码 |
| admin | boolean | 否 | 默认 false；true 为超管登录 |

**响应体（data）：** 与「2.2 登录」相同（token、userId、userType、roles、name、phone）。

---

### 2.4 微信小程序登录/注册

**接口含义：** 前端调用 `wx.login()` 获得临时 **code** 后，将 code 传给后端；后端用 code 调微信 jscode2session 换 openid，再根据 openid 查找或创建用户、更新昵称/头像等资料、写入登录流水，并签发 JWT。响应与手机号登录结构一致，并包含 avatarUrl（userType 可能为 WECHAT_USER，phone 可能为 null）。**不接收前端传入的 openid**，由后端通过 code 换取以保证安全。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/auth/wechat-login` |
| 认证 | 否 |
| Content-Type | `application/json` |

**请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| code | string | 是 | 微信临时登录凭证（前端 `wx.login()` 获得），后端用其调 jscode2session 换 openid |
| unionId | string | 否 | 微信 unionid（可选；也可由微信接口返回后后端写入） |
| nickName | string | 否 | 用户昵称（来自小程序头像昵称填写等，用于完善资料） |
| avatarUrl | string | 否 | 用户头像 URL |
| gender | number | 否 | 性别：0 未知，1 男，2 女 |
| country | string | 否 | 国家 |
| province | string | 否 | 省份 |
| city | string | 否 | 城市 |

**请求示例：**
```json
{
  "code": "0x1a2b3c4d...",
  "nickName": "用户昵称",
  "avatarUrl": "https://..."
}
```

**响应体（data）：** 与「2.2 登录」相同，并增加 avatarUrl（token、userId、userType、roles、name、phone、avatarUrl）。

---

### 2.5 设置密码（需已登录）

**接口含义：** 首次设置或修改密码。首次可不填 oldPassword；修改时须填原密码。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/auth/set-password` |
| 认证 | 是 |
| Content-Type | `application/json` |

**请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| newPassword | string | 是 | 新密码 |
| oldPassword | string | 否 | 原密码（修改时必填） |

**响应体：** 成功时 code 为 200，data 为 null。

---

### 2.6 当前用户（主持人 / 超管共用）

**接口含义：** 根据当前 token 获取当前登录用户信息，主持人与超管均调用此接口，响应中的 `userType`、`roles` 用于区分身份。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/auth/me` |
| 认证 | 是（主持人或超管 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为当前用户信息对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | number | 当前登录用户的用户 ID |
| userType | string | 主角色，如 HOST、SUPER_ADMIN |
| roles | string[] | 角色列表 |
| name | string | 用户姓名/昵称 |
| phone | string | 用户手机号 |
| avatarUrl | string | 用户头像 URL，可能为空 |

---

### 2.7 退出登录

**接口含义：** 主动退出登录，使当前 token 失效（若服务端实现 token 黑名单）。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/auth/logout` |
| 认证 | 是 |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体：** 无  

**响应体：** 成功时 `code` 为 200，`data` 为 null。

---

## 三、C 端 - 入口（免认证）

### 3.1 扫码入口 - 获取项目信息

**接口含义：** 用户通过分享链接/扫码进入时，凭分享 token 获取项目基本信息及**可选角色列表（roleOptions）**，无需登录。选择身份页由后端返回的 roleOptions 渲染。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/c/entry/{token}` |
| 认证 | 否 |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| token | string | 是 | 项目分享 token（即 shareToken），由「获取分享入口」接口得到 |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为项目信息对象，供 C 端身份选择页展示）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| projectId | number | 项目 ID |
| shareToken | string | 当前项目对应的分享令牌（与入口 URL 中一致） |
| groomName | string | 新郎姓名 |
| brideName | string | 新娘姓名 |
| theme | string | 婚礼主题，可为空 |
| weddingDate | string | 婚期，日期格式如 "2025-06-01" |
| roleOptions | array | **由后端返回的可选角色列表**，供前端渲染「选择身份」；每项见下表 |

**roleOptions 数组中每个元素：**

| 字段 | 类型 | 说明 |
|------|------|------|
| role | string | 角色枚举，如 GROOM、BRIDE |
| label | string | 展示名称，如 新郎、新娘 |
| available | boolean | 是否可选：true=未被占用可点选，false=已被占用仅展示 |

---

## 四、C 端 - 绑定与会话

以下接口为 **`/api/c/*`**，均需**登录**（C 端参与者 token）。

**会话与板块：** 绑定与进入采访已合并为 **`POST /api/c/sessions`**。**一个 session 仅对应「某用户在某板块下」的对话**，换板块会产生新 session，请求体必填 `projectId` + `projectBoardId`；未绑定时还需传 `role`（GROOM/BRIDE）。前端先调 **GET my-status** 得到当前步骤与 `currentProjectBoardId`（或 `sessionId`），再按需调 **POST /api/c/sessions** 进入某板块。

### 4.1 获取当前用户在该项目下的状态（my-status）

**接口含义：** C 端进入后**应先调本接口**。根据「当前用户 + 当前项目」返回用户在该项目下的步骤（step）、**板块信息（含板块 ID）**、会话 ID、待确认小结等，前端据此决定展示选身份、开始采访、对话页、小结确认页或完成页。

**与创建会话的串通：** 本接口返回的 **`currentProjectBoardId`** 为当前应进入的板块 ID；**`boards`** 为项目下全部板块列表，每项含 **`projectBoardId`**（板块 ID）、boardCode、boardName、displayOrder、isCurrent、isCompleted。后续**创建会话**一律基于 **项目 ID（即 my-status 的路径参数 projectId）+ 板块 ID（取 `currentProjectBoardId` 或 `boards[].projectBoardId`）** 调用 **POST /api/c/sessions**，body 传 `projectId`、`projectBoardId`（未绑定时再加 `role`）即可。

**会话按板块：** `sessionId` 表示当前应进入板块的会话（有则返回，无则为 null，此时用 `currentProjectBoardId` 调 POST /api/c/sessions）。C 端不暴露参与者 ID。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/c/projects/{projectId}/my-status` |
| 认证 | 是（C 端 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| projectId | number | 是 | 项目 ID（来自 C 端入口项目信息） |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| step | string | 当前步骤：`NOT_BOUND` / `BOUND_NO_SESSION` / `IN_CHAT` / `WAITING_SUMMARY_CONFIRM` / `ALL_COMPLETED` |
| bound | boolean | 是否已绑定该项目 |
| role | string | 角色：GROOM / BRIDE |
| participantStatus | string | 参与者状态 |
| sessionId | number | 当前应进入板块的会话 ID；若该板块尚无进行中会话则为 **null**，前端需用 `currentProjectBoardId` 调 POST /api/c/sessions |
| sessionStatus | string | 会话状态（有 sessionId 时有值） |
| currentProjectBoardId | number | 当前应进入的板块 ID（与 sessionId 对应；BOUND_NO_SESSION 时用于调 POST /api/c/sessions） |
| boardCode | string | 当前板块编码 |
| boardName | string | 当前板块名称 |
| currentBoardOrder | number | 当前板块顺序号 |
| boards | array | 项目下各板块简要信息，**每项含 projectBoardId（板块 ID）**，用于创建会话或展示进度；结构见下表 |
| currentBoardRoundCount | number | 当前板块已用轮数 |

**boards 数组中每个元素：**

| 字段 | 类型 | 说明 |
|------|------|------|
| projectBoardId | number | 板块 ID，**创建会话时 body.projectBoardId 取此值或 currentProjectBoardId** |
| boardCode | string | 板块编码 |
| boardName | string | 板块名称 |
| displayOrder | number | 展示顺序，数值越小越靠前 |
| isCurrent | boolean | 是否为当前应进入的板块 |
| isCompleted | boolean | 该板块是否已完成 |
| maxRoundsPerBoard | number | 每块轮数上限 |
| currentSummaryId | number | 待确认小结 ID（仅 WAITING_SUMMARY_CONFIRM 时有值） |
| currentSummaryStatus | string | 当前小结状态 |

**前端用法：** 根据 `step` 分支：`NOT_BOUND` → 选身份后调 **POST /api/c/sessions**，body 传 `projectId` + `projectBoardId` + `role`（GROOM/BRIDE）；`BOUND_NO_SESSION` → 调 **POST /api/c/sessions**，body 传 `projectId` + `projectBoardId`（取本接口返回的 `currentProjectBoardId`）；`IN_CHAT` → 用 `sessionId` 进对话/发消息/提交/生成小结；`WAITING_SUMMARY_CONFIRM` → 用 `currentSummaryId` 进小结确认；`ALL_COMPLETED` → 展示完成页。

**典型流程（会话按板块）：**
1. **GET my-status** → 若 `step = BOUND_NO_SESSION` 且 `sessionId == null`，用返回的 `currentProjectBoardId` 作为要进入的板块。
2. **POST /api/c/sessions** → body：`projectId` + `projectBoardId`（+ 未绑定时 `role`），得到该板块的 `sessionId`。
3. 对话、提交、小结等均使用该 `sessionId`；**换板块时**再次调 **POST /api/c/sessions** 传下一板块的 `projectBoardId`，得到新 `sessionId`。

---

### 4.2 获取语音转写配额（C 端）

**接口含义：** 获取当前登录用户的语音转写剩余秒数与已使用秒数，用于 C 端语音识别功能展示。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/c/speech-quota` |
| 认证 | 是（C 端 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**：无路径/Query/体。

**响应体（data）：** 含 `remainingSeconds`、`totalUsedSeconds` 等（以实际 DTO 为准）。

---

### 4.2.1 实时语音转写（WebSocket，C 端）

**接口含义：** 边说边转写。客户端建立 WebSocket 连接后发送 PCM 音频流，服务端实时返回转写文本。用于 C 端语音对话场景，与「发送消息」「提交会话」串联使用。详细流程与音频格式要求见《C端语音对话接口文档》。

| 项目 | 说明 |
|------|------|
| 协议 | WebSocket |
| 连接地址（开发） | `ws://{host}/api/c/ws/speech-recognition` |
| 连接地址（生产） | `wss://{host}/api/c/ws/speech-recognition` |
| 认证 | 握手时 Header：`Authorization: Bearer <access_token>`；部分环境不支持自定义 Header 时可用 URL 参数：`?token=<access_token>`（若后端已支持） |

**客户端 → 服务端：**

| 消息类型 | 说明 |
|----------|------|
| Binary | PCM 音频包：16kHz、16bit、单声道；建议每包 100～200ms（约 3200 字节/100ms） |
| Text | 结束本次录音：内容为 `"end"` 或 `"stop"` |

**服务端 → 客户端：**

| 消息类型 | 说明 | 格式 |
|----------|------|------|
| Text | 转写结果（增量或完整） | `{"type":"transcript","text":"转写内容"}` |
| Text | 错误信息 | `{"type":"error","message":"错误描述"}` |

**转写示例：** `{"type":"transcript","text":"我的童年是在农村度过的"}`  
**错误示例：** `{"type":"error","message":"连接已断开"}`

---

### 4.3 进入项目采访（绑定 + 创建/恢复会话）

**接口含义：** 一步完成「选身份绑定 + 创建或恢复**该板块**的会话」。**会话按板块拆分**：一个 session 仅对应「某用户在某板块下」的对话，换板块会产生新 session。若用户**尚未绑定**该项目，请求体必填 `role`（GROOM/BRIDE）；**必填** `projectBoardId` 表示进入该项目下哪一个板块的采访。返回会话对象（含 `role`）供后续消息、小结等使用。**不传 sessionId**：若该用户在该板块下已有进行中/待确认会话则返回该会话（恢复），否则新建会话；响应 data.id 即为后续要用的 sessionId。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/c/sessions` |
| 认证 | 是（C 端 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| projectId | number | 是 | 项目 ID（与 **my-status 路径参数**一致） |
| projectBoardId | number | 是 | 板块 ID，表示进入该项目下哪一个板块的采访；**从 my-status 的 currentProjectBoardId 或 boards[].projectBoardId 取得**；换板块会生成新 session |
| role | string | 条件必填 | 角色：`GROOM`（新郎）、`BRIDE`（新娘）。**未绑定时必填**，已绑定可省略 |

**请求示例（未绑定，首次选身份进入某板块）：**
```json
{
  "projectId": 3,
  "projectBoardId": 10,
  "role": "GROOM"
}
```

**请求示例（已绑定，进入/恢复某板块会话）：**
```json
{
  "projectId": 3,
  "projectBoardId": 10
}
```

**错误：** 未绑定且未传 `role` 或 `role` 为空时，返回 400，提示「请选择身份（新郎/新娘）」；该身份已被占用时返回 400「该身份已被占用，请选择其他身份」；板块不属于当前项目时返回 400。

**响应体（data 为会话对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 会话 ID |
| projectId | number | 所属项目 ID |
| role | string | 当前用户在该项目中的角色：GROOM / BRIDE |
| currentProjectBoardId | number | 本会话所属板块 ID（一个 session 仅对应一个板块） |
| boardCode | string | 当前板块的编码 |
| boardName | string | 当前板块的名称 |
| status | string | 会话状态（如进行中、已提交等） |
| roundCount | number | 当前会话已进行的总轮数 |
| currentBoardRoundCount | number | 当前板块已使用的轮数 |
| maxRoundsPerBoard | number | 每个板块允许的最大轮数上限 |
| startedAt | string | 会话开始时间，ISO 日期时间 |
| lastActiveAt | string | 最后活动时间，ISO 日期时间 |
| createdAt | string | 会话创建时间，ISO 日期时间 |
| currentBoardOrder | number | 当前板块在项目中的顺序号（从 1 起） |
| boards | array | 当前项目下所有板块的简要信息列表，见下表 |

**boards 数组中每个元素：**

| 字段 | 类型 | 说明 |
|------|------|------|
| projectBoardId | number | 项目板块 ID |
| boardCode | string | 板块编码 |
| boardName | string | 板块名称 |
| displayOrder | number | 展示顺序，数值越小越靠前 |
| isCurrent | boolean | 是否为当前进行中的板块 |
| isCompleted | boolean | 该板块是否已完成 |

---

### 4.4 获取会话

**接口含义：** 根据会话 ID 查询会话详情及状态。该 session 仅对应一个板块（`currentProjectBoardId` 即本会话所属板块）；响应中的 `boards` 为项目下全部板块列表（用于进度展示），其中仅本会话板块 `isCurrent === true`。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/c/sessions/{sessionId}` |
| 认证 | 是（C 端 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | number | 是 | 会话 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为会话对象）：** 字段与「4.3 进入项目采访」的 data 完全相同（含 role），见上表。

---

### 4.5 获取消息列表

**接口含义：** 分页或全量获取指定会话下的消息列表（采访对话记录）。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/c/sessions/{sessionId}/messages` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | number | 是 | 会话 ID |

- **Query 参数：** 无（若支持分页，以实际后端为准）  
- **响应体（data 为对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| messages | array | 消息列表，按序号排序，元素结构见下表 |
| totalRounds | number | 会话总轮数（已提交的对话轮次） |

**messages 数组元素结构：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 消息 ID |
| sessionId | number | 所属会话 ID |
| senderType | string | 发送方类型（如用户、系统） |
| messageType | string | 消息类型（如文本、语音等） |
| content | string | 消息正文内容 |
| audioUrl | string | 关联的语音文件 URL，无则为 null |
| sequenceNo | number | 消息在会话中的序号 |
| batchNo | number | 消息所属批次号（与提交轮次相关） |
| isSubmitted | boolean | 该条消息是否已随会话提交 |
| createdAt | string | 消息创建时间，ISO 日期时间 |

---

### 4.6 发送消息

**接口含义：** 在指定会话中发送一条消息（文字或关联语音/转写），用于采访过程中的对话记录。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/c/sessions/{sessionId}/messages` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | number | 是 | 会话 ID |

- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| content | string | 否 | 消息正文，最长 5000 字符 |
| audioUrl | string | 否 | 关联语音文件 URL |
| transcriptText | string | 否 | 语音转写文本 |

**请求示例：**
```json
{
  "content": "测试消息内容",
  "audioUrl": null,
  "transcriptText": null
}
```

**响应体（data 为单条消息对象）：** 字段与「4.5 获取消息列表」中数组元素结构相同。

---

### 4.7 更新消息

**接口含义：** 修改指定会话下某条消息的内容（仅支持改 content）。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/c/sessions/{sessionId}/messages/{messageId}` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | number | 是 | 会话 ID |
| messageId | number | 是 | 消息 ID |

- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| content | string | 否 | 更新后的正文，最长 5000 字符 |

**请求示例：**
```json
{
  "content": "更新后的内容"
}
```

**响应体（data 为更新后的消息对象）：** 字段与「4.5 获取消息列表」中数组元素结构相同。

---

### 4.8 删除消息

**接口含义：** 删除指定会话下的一条消息。

| 项目 | 说明 |
|------|------|
| 方法 | `DELETE` |
| 路径 | `/api/c/sessions/{sessionId}/messages/{messageId}` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | number | 是 | 会话 ID |
| messageId | number | 是 | 消息 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体：** 成功时 `code` 为 200，`data` 为 null。

---

### 4.9 提交会话

**接口含义：** 将当前会话标记为已提交，表示采访结束，后续可能用于生成小结或交付物。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/c/sessions/{sessionId}/submit` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | number | 是 | 会话 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为提交结果对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| newBatchNo | number | 本轮提交后产生的新批次号 |
| roundCount | number | 提交后会话的累计已完成轮数 |
| maxRoundsPerBoard | number | 当前板块最大轮数（配置值，即该板块最多可对话多少轮） |
| newMessages | array | 本轮新生成的消息列表，每项结构同「4.5 获取消息列表」中的消息对象 |

---

### 4.10 流式提交（WebSocket）

**接口含义：** 通过 WebSocket 提交当前会话并**流式接收** AI 回复。与「4.9 提交会话」业务等价（校验、标记已提交、调用大模型、落库），区别在于 AI 回复以增量形式推送，便于前端做打字机效果、降低首字延迟。需先建立 WebSocket 连接，连接成功后发送一条包含 `sessionId` 的文本消息即可触发展开。

| 项目 | 说明 |
|------|------|
| 协议 | WebSocket |
| 连接地址（开发） | `ws://{host}/api/c/ws/submit-stream` |
| 连接地址（生产） | `wss://{host}/api/c/ws/submit-stream` |
| 认证 | 握手时 URL 参数：`?token={JWT}`（与 C 端登录后获得的 token 一致） |

**握手示例：** `ws://localhost:8080/api/c/ws/submit-stream?token=eyJhbGciOiJIUzI1NiIs...`

**客户端 → 服务端（连接成功后发送一条文本消息）：**

| 格式 | 说明 |
|------|------|
| JSON 文本 | 必须包含 `sessionId`，为当前采访会话 ID |

**请求示例：**
```json
{"sessionId": 1}
```

**服务端 → 客户端（下行均为 JSON 文本）：**

| type | 说明 | 示例 |
|------|------|------|
| delta | AI 回复的增量片段，可多次推送 | `{"type":"delta","content":"感谢"}`、`{"type":"delta","content":"你的分享。"}` |
| done | 流结束，并已落库；含本条 AI 消息 ID 与当前轮数 | `{"type":"done","messageId":5,"roundCount":1}` |
| error | 校验或服务异常 | `{"type":"error","message":"暂无待提交的消息"}` |

**错误场景示例：** 暂无待提交消息、未选定板块、轮数达上限、无权限等均会推送 `{"type":"error","message":"..."}`。

**前端流程建议：** 1）建立 WebSocket（带 token）；2）发送 `{"sessionId": sessionId}`；3）循环接收：收到 `delta` 则追加展示，收到 `done` 则更新消息列表/轮数并结束，收到 `error` 则提示并结束。

---

## 五、C 端 - 小结

**说明：** 小结用于整理采访中的**事实（核心要点）**与**关键人物**。**创建即确认**：调用「创建小结」时，后端根据当前会话对话由 AI 生成结构化小结并**立即确认**（勾选条目写入素材快照、推进板块进度），不再区分「生成小结」与「确认小结」两个步骤。返回小结对象的接口（5.1 创建、5.2 获取当前、5.3 获取详情）的 `data` 均包含 `items`（事实/表达条目列表）与 `keyPersons`（本小结绑定的关键人物列表）。

### 5.1 创建小结（生成即确认）

**接口含义：** 针对当前会话创建小结并立即确认。后端根据会话消息调用 AI 生成结构化小结（事实 + 关键人物），解析入库后**直接确认**（将条目写入素材快照、推进参与者板块进度、标记会话完成），返回完整小结对象（含 `items`、`keyPersons`，状态为已确认）。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/c/sessions/{sessionId}/summaries` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | number | 是 | 会话 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为小结对象）：** 结构同「5.3 获取小结详情」的 data，见该节。

---

### 5.2 获取当前小结

**接口含义：** 获取该会话下当前有效的一份小结（未确认或最新一次）。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/c/sessions/{sessionId}/summaries/current` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | number | 是 | 会话 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为小结对象）：** 结构同「5.3 获取小结详情」的 data。

---

### 5.3 获取小结详情

**接口含义：** 根据小结 ID 查询小结详情及条目列表。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/c/board-summaries/{summaryId}` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| summaryId | number | 是 | 小结 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为板块小结对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 小结 ID |
| sessionId | number | 所属会话 ID |
| projectBoardId | number | 所属项目板块 ID |
| boardCode | string | 板块编码 |
| boardName | string | 板块名称 |
| versionNo | number | 小结版本号 |
| status | string | 小结状态（如未确认、已确认等） |
| title | string | 小结标题，可能为空 |
| generatedAt | string | 小结生成时间，ISO 日期时间 |
| confirmedAt | string | 小结确认时间，ISO 日期时间，未确认时为 null |
| items | array | 小结条目列表（事实/表达），每项结构见下表 |
| keyPersons | array | 本小结绑定的关键人物列表，每项结构见下表 |

**items 数组中每个元素：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 小结条目 ID |
| summaryId | number | 所属小结 ID |
| itemType | string | 条目类型：FACT（事实类）、EXPRESSION（表达类） |
| content | string | 条目正文内容 |
| itemOrder | number | 条目在小结中的展示顺序 |
| isSelected | boolean | 是否被勾选参与后续生成 |
| createdAt | string | 条目创建时间，ISO 日期时间 |

**keyPersons 数组中每个元素：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 关键人物 ID |
| userId | number | 所属用户 ID |
| sessionId | number | 所属会话 ID，可选 |
| name | string | 人物称呼或姓名 |
| roleLabel | string | 关系/角色标签（如亲密关系、家族根源） |
| createdAt | string | 创建时间，ISO 日期时间 |

---

### 5.4 添加小结条目

**接口含义：** 在小结中新增一条事实或表达类条目，用于人工补充或修正。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/c/board-summaries/{summaryId}/items` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| summaryId | number | 是 | 小结 ID |

- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| content | string | 是 | 条目内容，最长 500 字符 |
| itemType | string | 否 | 条目类型，默认 `FACT`。可选：`FACT`（事实类）、`EXPRESSION`（表达类） |

**请求示例：**
```json
{
  "content": "事实内容",
  "itemType": "FACT"
}
```

**响应体（data 为单条小结条目对象）：** 字段与「5.3 获取小结详情」中 items 数组元素结构相同。

---

### 5.5 更新小结条目

**接口含义：** 修改某条小结条目的内容、类型或是否被选中（用于生成内容时的勾选）。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/c/summary-items/{itemId}` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| itemId | number | 是 | 小结条目 ID |

- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| content | string | 否 | 更新后的内容，最长 500 字符 |
| itemType | string | 否 | 类型：`FACT`、`EXPRESSION` |
| isSelected | boolean | 否 | 是否被选中参与生成 |

**请求示例：**
```json
{
  "content": "更新内容",
  "itemType": "FACT",
  "isSelected": true
}
```

**响应体（data 为更新后的条目对象）：** 字段与「5.3 获取小结详情」中 items 数组元素结构相同。

---

### 5.6 删除小结条目

**接口含义：** 删除指定小结条目。

| 项目 | 说明 |
|------|------|
| 方法 | `DELETE` |
| 路径 | `/api/c/summary-items/{itemId}` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| itemId | number | 是 | 小结条目 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体：** 成功时 `code` 为 200，`data` 为 null。

---

### 5.7 设置小结关键人物

**接口含义：** 设置本小结绑定的关键人物列表（从当前用户/会话已有的关键人物中勾选）。未确认的小结可修改绑定；已确认后不可修改。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/c/board-summaries/{summaryId}/key-persons` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| summaryId | number | 是 | 小结 ID |

- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyPersonIds | array | 是 | 关键人物 ID 列表，须为当前用户已有的关键人物 ID |

**请求示例：**
```json
{
  "keyPersonIds": [1, 2, 3]
}
```

**响应体：** 成功时 `code` 为 200，`data` 为 null。之后调用「获取小结详情」或「获取当前小结」时，该小结的 `keyPersons` 即为本次设置的列表。

---

## 六、C 端 - 故事与人物

**场景说明：** 故事面向 C 端用户，用于查询**自己的全部故事内容**。用户可查看全部故事，或按**板块**、**某次会话**、**某角色（关键人物）**等条件筛选。

---

### 6.1 我的板块列表

**接口含义：** 当前用户有故事的项目板块列表，用于故事页按板块 Tab/筛选。仅返回用户有故事记录的板块；按某板块查故事时可用返回的 `projectBoardId` 传「我的故事列表」的 `projectBoardId` 参数，或用 `boardCode` 传「我的故事列表」的 `boardCode` 参数（按板块类型跨项目汇总）。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/c/users/me/boards` |
| 认证 | 是（C 端用户 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为板块对象数组）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| projectId | number | 项目 ID |
| projectBoardId | number | 项目板块 ID，按该板块筛故事时传「我的故事列表」的 projectBoardId |
| boardCode | string | 板块类型编码，按板块类型筛故事时传「我的故事列表」的 boardCode |
| boardName | string | 板块名称 |
| displayOrder | number | 展示顺序 |

---

### 6.2 我的故事列表（分页）

**接口含义：** 当前用户查询自己的全部故事，支持按会话、板块、角色筛选，分页返回；不传筛选参数时返回该用户在所有项目下的全部故事（分页）。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/c/users/me/stories` |
| 认证 | 是（C 端用户 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：** 无  
- **Query 参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | number | 否 | 限定某次会话下的故事 |
| projectBoardId | number | 否 | 限定某项目板块下的故事 |
| boardCode | string | 否 | 限定板块类型编码（如 FIRST_MEET） |
| roleLabel | string | 否 | 限定包含该角色的会话下的故事（关键人物的角色标签，如「新郎父亲」） |
| page | number | 否 | 页码，从 1 开始，默认 1 |
| size | number | 否 | 每页条数，默认 10，最大 50 |

- **请求体：** 无  

**响应体（data 为分页对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| content | array | 当前页的故事对象列表，按创建时间倒序 |
| totalElements | number | 总记录数（跨所有页） |
| totalPages | number | 总页数 |
| size | number | 当前页每页条数 |
| number | number | 当前页码（从 0 开始，即第 1 页为 0） |
| first | boolean | 是否为第一页 |
| last | boolean | 是否为最后一页 |
| numberOfElements | number | 当前页实际条数 |
| empty | boolean | 当前页是否无数据 |

**content 中每项（故事对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 故事 ID |
| sessionId | number | 所属会话 ID |
| projectId | number | 所属项目 ID |
| projectBoardId | number | 所属项目板块 ID |
| boardCode | string | 板块编码 |
| boardName | string | 板块名称 |
| content | string | 故事正文内容 |
| versionNo | number | 故事版本号 |
| createdAt | string | 故事创建时间，ISO 日期时间 |

---

### 6.3 创建故事

**接口含义：** 基于当前会话的**当前板块**对话（及小结）创建或初始化该板块的“故事”数据，用于后续婚礼故事/文案。需已进入某板块并有对话后再调用。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/c/sessions/{sessionId}/stories` |
| 认证 | 是（C 端用户 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | number | 是 | 会话 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为故事对象）：** 结构同「6.2 我的故事列表」中数组元素。

**错误：** 当前会话未选定板块或无板块时返回 400（如「当前无板块」）。

---

### 6.4 按会话+板块获取故事（单条）

**接口含义：** 按会话与项目板块查询对应的单条故事内容（用于详情或会话内某板块故事）。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/c/sessions/{sessionId}/stories` |
| 认证 | 是（C 端用户 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | number | 是 | 会话 ID |

- **Query 参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| projectBoardId | number | 是 | 项目板块 ID |

- **请求体：** 无  

**响应体（data 为故事对象，无则 null）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 故事 ID |
| sessionId | number | 所属会话 ID |
| projectId | number | 所属项目 ID |
| projectBoardId | number | 所属项目板块 ID |
| boardCode | string | 板块编码 |
| boardName | string | 板块名称 |
| content | string | 故事正文内容 |
| versionNo | number | 故事版本号 |
| createdAt | string | 故事创建时间，ISO 日期时间 |

---

### 6.4 获取关键人物列表

**接口含义：** 获取当前会话下已添加的关键人物列表（如新郎父亲、新娘母亲等），用于故事与文案生成。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/c/sessions/{sessionId}/persons` |
| 认证 | 是（C 端用户 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | number | 是 | 会话 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为关键人物对象数组）：** 每项结构同「6.6 更新关键人物」返回的 data，见该节。

---

### 6.5 添加关键人物

**接口含义：** 在当前会话下新增一位关键人物（称谓 + 角色标签）。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/c/sessions/{sessionId}/persons` |
| 认证 | 是（C 端用户 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | number | 是 | 会话 ID |

- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | string | 是 | 人物称谓，如「新郎父亲」，最长 50 字符 |
| roleLabel | string | 否 | 角色标签，如「家人」，最长 50 字符 |

**请求示例：**
```json
{
  "name": "新郎父亲",
  "roleLabel": "家人"
}
```

**响应体（data 为新增的关键人物对象）：** 字段与「6.6 更新关键人物」的 data 相同。

---

### 6.6 更新关键人物

**接口含义：** 修改已有关键人物的称谓或角色标签。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/c/key-persons/{personId}` |
| 认证 | 是（C 端用户 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| personId | number | 是 | 关键人物 ID |

- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | string | 否 | 称谓，最长 50 字符 |
| roleLabel | string | 否 | 角色标签，最长 50 字符 |

**请求示例：**
```json
{
  "name": "新郎父亲(更新)",
  "roleLabel": "家人"
}
```

**响应体（data 为关键人物对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 关键人物 ID |
| sessionId | number | 所属会话 ID |
| name | string | 人物称谓（如新郎父亲） |
| roleLabel | string | 角色标签（如家人） |
| createdAt | string | 创建时间，ISO 日期时间 |

---

### 6.7 删除关键人物

**接口含义：** 删除指定关键人物。

| 项目 | 说明 |
|------|------|
| 方法 | `DELETE` |
| 路径 | `/api/c/key-persons/{personId}` |
| 认证 | 是（C 端用户 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| personId | number | 是 | 关键人物 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体：** 成功时 `code` 为 200，`data` 为 null。

---

## 七、项目（主持人 / 超管共用）

以下接口路径为 **`/api/projects`**。主持人（HOST）仅能操作本人项目；超管（SUPER_ADMIN）可查全部项目、任意项目详情与分享入口。需认证：`Authorization: Bearer {{token}}`。

### 7.1 创建项目

**接口含义：** 主持人创建一个新婚礼项目，填写新郎新娘姓名与婚期。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/projects` |
| 认证 | 是（主持人或超管 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| groomName | string | 是 | 新郎姓名，2～50 字符 |
| brideName | string | 是 | 新娘姓名，2～50 字符 |
| weddingDate | string | 否 | 婚期，日期格式如 `2025-06-01`（LocalDate） |

**请求示例：**
```json
{
  "groomName": "张先生",
  "brideName": "李女士",
  "weddingDate": "2025-06-01"
}
```

**响应体（data 为项目详情对象）：** 结构同「7.3 项目详情」的 data，见该节。

---

### 7.2 项目列表

**接口含义：** 分页查询项目列表。主持人返回本人项目；超管可传 `hostUserId`、`status`、`keyword` 等筛选查全部。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/projects` |
| 认证 | 是（主持人或超管 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：** 无  
- **Query 参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | number | 否 | 页码，从 1 开始，默认 1 |
| size | number | 否 | 每页条数，默认 10 |
| hostUserId | number | 否 | **仅超管**：按主持人 ID 筛选 |
| status | string | 否 | **仅超管**：项目状态 |
| keyword | string | 否 | **仅超管**：关键词（新人姓名、项目编号） |

- **请求体：** 无  

**响应体（data 为分页对象）：** 遵循文档开头的「分页响应」结构。主持人：`content` 为项目列表项；超管：`content` 为含 hostName、hostPhone 等的列表项。结构见下表。

**content 数组中每个元素：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 项目 ID |
| projectNo | string | 项目编号（业务展示用） |
| groomName | string | 新郎姓名 |
| brideName | string | 新娘姓名 |
| weddingDate | string | 婚期，日期格式 |
| status | string | 项目状态 |
| createdAt | string | 项目创建时间，ISO 日期时间 |

---

### 7.3 项目详情

**接口含义：** 根据项目 ID 查询项目详情。主持人仅能查本人项目；超管可查任意项目（返回含 hostUserId、hostName、hostPhone）。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/projects/{id}` |
| 认证 | 是（主持人或超管 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 项目 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为项目详情对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 项目 ID |
| projectNo | string | 项目编号 |
| groomName | string | 新郎姓名 |
| brideName | string | 新娘姓名 |
| theme | string | 婚礼主题，可为空 |
| weddingDate | string | 婚期，日期格式 |
| contactInfo | string | 联系方式（与项目绑定），可为空 |
| status | string | 项目状态 |
| shareToken | string | 项目分享令牌 |
| createdAt | string | 项目创建时间，ISO 日期时间 |
| participants | array | 成员列表（新郎、新娘等参与者摘要），每项见下表 |
| contents | array | 生成物摘要列表，每项见下表 |

**超管调用时** data 额外包含：`hostUserId`（number，主持人用户 ID）、`hostName`（string）、`hostPhone`（string）。

**participants 每项：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 参与者 ID |
| role | string | 角色标识，如 GROOM、BRIDE |
| roleType | string | 角色类型 |
| roleName | string | 角色展示名，如新郎、新娘 |
| status | string | 参与者状态 |
| currentBoardOrder | number | 当前进行到的板块顺序（1～4 等） |
| joinedAt | string | 加入时间，ISO 日期时间 |
| lastActiveAt | string | 最后活动时间，ISO 日期时间 |
| bound | boolean | 是否已绑定用户（是否已有用户选该身份并绑定） |
| boardMaterials | array | 各板块下当前最新**素材快照**（小结确认后的勾选条目），按板块 displayOrder 排序；每项见「participants.boardMaterials 每项」 |
| boardStories | array | 各板块下的**故事**（用于详情页展示故事内容），按板块 displayOrder 排序；每项见「participants.boardStories 每项」 |

**participants.boardMaterials 每项：**

| 字段 | 类型 | 说明 |
|------|------|------|
| projectBoardId | number | 项目板块 ID |
| boardCode | string | 板块编码 |
| boardName | string | 板块名称 |
| displayOrder | number | 板块展示顺序 |
| snapshotPayload | string | 快照内容（JSON 数组，如 [{"content":"...","type":"..."}]） |
| snapshotCreatedAt | string | 快照创建时间，ISO 日期时间 |
| summaryId | number | 关联小结 ID |

**participants.boardStories 每项：**

| 字段 | 类型 | 说明 |
|------|------|------|
| projectBoardId | number | 项目板块 ID |
| boardCode | string | 板块编码 |
| boardName | string | 板块名称 |
| displayOrder | number | 板块展示顺序 |
| id | number | 故事 ID（BoardStory.id） |
| content | string | 故事正文 |
| versionNo | number | 故事版本号 |
| createdAt | string | 故事创建时间，ISO 日期时间 |

**contents 每项：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 生成物 ID |
| contentType | string | 类型：OPENING_SPEECH、GROOM_VOW、BRIDE_VOW |
| contentTypeName | string | 类型中文名：开场白、新郎誓言、新娘誓言 |
| title | string | 标题 |
| status | string | 状态：DRAFT、ACTIVE、OUTDATED 等 |
| usingLatestSnapshot | boolean | 是否基于最新素材快照生成：true=当前无新快照，false=已有新快照待重新生成 |
| versionNo | number | 版本号 |
| updatedAt | string | 更新时间，ISO 日期时间 |

---

### 7.4 获取分享入口

**接口含义：** 获取该项目的分享链接所需 token（或短链），用于 C 端「扫码入口 - 获取项目信息」。主持人仅本人项目；超管可查任意项目。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/projects/{id}/share` |
| 认证 | 是（主持人或超管 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 项目 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为分享入口对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| projectId | number | 项目 ID |
| shareToken | string | 分享令牌，C 端「扫码入口 - 获取项目信息」接口的路径参数即此值 |
| entryUrl | string | C 端扫码/点击进入的入口 URL |
| qrCodeUrl | string | 二维码图片 URL，用于生成二维码供扫码 |
| wechatScheme | string | 可选。微信小程序 URL Scheme（`weixin://dl/business/?t=xxx`），用于分享到微信内打开小程序；已配置小程序且生成成功时返回，否则为 null |
| wechatSchemeQrCodeUrl | string | 可选。基于 wechatScheme 生成的二维码图片 URL，便于下载后发微信；无 scheme 时为 null |

---

## 八、项目下板块（主持人 / 超管）

以下为 **`/api/projects/{projectId}/boards`**，主持人仅能操作本人项目；超管可按需开放。需认证。

### 8.1 板块列表

**接口含义：** 查询某项目下已添加的板块列表（采访主题/环节）。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/projects/{projectId}/boards` |
| 认证 | 是（主持人或超管 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| projectId | number | 是 | 项目 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为板块对象数组）：** 每项结构见下表。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 项目板块 ID（本条记录在项目中的唯一 ID） |
| projectId | number | 项目 ID |
| boardMetaId | number | 板块元数据 ID |
| boardCode | string | 板块编码 |
| boardName | string | 板块名称 |
| displayOrder | number | 展示顺序，数值越小越靠前 |
| createdAt | string | 创建时间，ISO 日期时间 |

---

### 8.2 添加板块

**接口含义：** 为项目添加一个板块（从系统板块元数据中选择一种类型并指定顺序）。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/projects/{projectId}/boards` |
| 认证 | 是（主持人或超管 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| projectId | number | 是 | 项目 ID |

- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| boardMetaId | number | 是 | 板块元数据 ID（来自「板块元数据-启用列表」） |
| displayOrder | number | 否 | 展示顺序，默认 0，数值越小越靠前 |

**请求示例：**
```json
{
  "boardMetaId": 1,
  "displayOrder": 0
}
```

**响应体（data 为新增的板块对象）：** 字段与「8.1 板块列表」中数组元素结构相同。

---

### 8.3 更新板块

**接口含义：** 调整项目板块的展示顺序等。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/projects/{projectId}/boards/{projectBoardId}` |
| 认证 | 是（主持人或超管 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| projectId | number | 是 | 项目 ID |
| projectBoardId | number | 是 | 项目板块 ID（本条板块在项目中的 ID） |

- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| displayOrder | number | 否 | 新的展示顺序 |

**请求示例：**
```json
{
  "displayOrder": 1
}
```

**响应体（data 为更新后的板块对象）：** 字段与「8.1 板块列表」中数组元素结构相同。

---

### 8.4 删除板块

**接口含义：** 从项目中移除指定板块。

| 项目 | 说明 |
|------|------|
| 方法 | `DELETE` |
| 路径 | `/api/projects/{projectId}/boards/{projectBoardId}` |
| 认证 | 是（主持人或超管 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| projectId | number | 是 | 项目 ID |
| projectBoardId | number | 是 | 项目板块 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体：** 成功时 `code` 为 200，`data` 为 null。

---

### 8.5 板块元数据（统一路径）

**接口含义：** 板块元数据统一为 **`/api/board-meta`**。`GET /api/board-meta`：主持人仅返回已启用列表，超管返回全部；`GET /api/board-meta/enabled` 仅返回已启用；增删改仅超管。详见「十三、板块元数据」。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/board-meta` 或 `/api/board-meta/enabled` |
| 认证 | 是（主持人或超管 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为板块元数据对象数组）：** 每项含 id、code、name、displayOrder、description、status 等；主持人调用时仅返回已启用（status=ENABLED）。

---

### 8.6 交付物元数据（统一路径）

**接口含义：** 交付物元数据统一为 **`/api/deliverable-meta`**。`GET /api/deliverable-meta`：主持人仅返回已启用列表，超管返回全部；`GET /api/deliverable-meta/enabled` 仅返回已启用；增删改仅超管。详见「十三（续）、交付物元数据」。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/deliverable-meta` 或 `/api/deliverable-meta/enabled` |
| 认证 | 是（主持人或超管 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为交付物元数据对象数组）：** 每项含 id、code、name、displayOrder、description、status 等；主持人调用时仅返回已启用（status=ENABLED）。

---

## 九、交付物（主持人 / 超管）

### 9.1 生成交付物

**接口含义：** 按类型为项目生成一份交付物（如婚礼开场白、新郎誓言、新娘誓言），会消耗配额并可能异步生成。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/projects/{projectId}/deliverables/generate` |
| 认证 | 是（主持人或超管 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| projectId | number | 是 | 项目 ID |

- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| contentType | string | 是 | 交付物类型。可选：`OPENING_SPEECH`（婚礼开场白）、`GROOM_VOW`（新郎誓言）、`BRIDE_VOW`（新娘誓言） |

**请求示例：**
```json
{
  "contentType": "OPENING_SPEECH"
}
```

**响应体（data 为交付物详情对象）：** 结构同「9.3 按 ID 获取交付物」的 data，见该节。

---

### 9.2 按类型获取交付物

**接口含义：** 根据项目 ID 与交付物类型查询该类型下已有的交付物（通常为最新一份）。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/projects/{projectId}/deliverables/{contentType}` |
| 认证 | 是（主持人或超管 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| projectId | number | 是 | 项目 ID |
| contentType | string | 是 | 类型：`OPENING_SPEECH`、`GROOM_VOW`、`BRIDE_VOW` |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为交付物对象）：** 字段与「9.3 按 ID 获取交付物」的 data 相同；若该类型下无交付物可能返回 404 或空。

---

### 9.3 按 ID 获取交付物

**接口含义：** 根据交付物 ID 查询详情。路径为 **`/api/deliverables/{id}`**。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/deliverables/{id}` |
| 认证 | 是（主持人或超管 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 交付物 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为交付物详情对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 交付物 ID |
| projectId | number | 所属项目 ID |
| contentType | string | 类型：OPENING_SPEECH、GROOM_VOW、BRIDE_VOW |
| contentTypeName | string | 类型中文名：婚礼开场白、新郎誓言、新娘誓言 |
| versionNo | number | 版本号 |
| title | string | 标题 |
| content | string | 正文内容 |
| status | string | 状态（如草稿、已发布等） |
| usingLatestSnapshot | boolean | 是否基于最新素材快照生成：true=当前无新快照，false=已有新快照待重新生成 |
| snapshotVersionAt | string | 快照版本时间，ISO 日期时间 |
| createdAt | string | 创建时间，ISO 日期时间 |
| updatedAt | string | 更新时间，ISO 日期时间 |

---

### 9.4 更新交付物

**接口含义：** 修改交付物的标题或正文内容。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/deliverables/{id}` |
| 认证 | 是（主持人或超管 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 交付物 ID |

- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| title | string | 否 | 标题，最长 100 字符 |
| content | string | 否 | 正文内容 |

**请求示例：**
```json
{
  "title": "婚礼开场白",
  "content": "尊敬的各位来宾..."
}
```

**响应体（data 为更新后的交付物对象）：** 字段与「9.3 按 ID 获取交付物」的 data 相同。

---

### 9.5 删除交付物

**接口含义：** 删除指定交付物。

| 项目 | 说明 |
|------|------|
| 方法 | `DELETE` |
| 路径 | `/api/deliverables/{id}` |
| 认证 | 是（主持人或超管 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 交付物 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体：** 成功时 `code` 为 200，`data` 为 null。

---

## 十、用户 / 当前用户资料（主持人 / 超管共用）

以下为 **`/api/users`**。`GET/PUT /api/users/me` 共用：主持人返回 HostProfileResponse（昵称、会员、配额等），超管返回 CurrentUserResponse；列表、详情、状态、会员、配额等仅超管。

### 10.1 我的资料

**接口含义：** 查询当前用户的个人资料。主持人为昵称、手机号、联系方式、配额等；超管为基本信息（userId、userType、roles、name、phone）。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/users/me` |
| 认证 | 是（主持人或超管 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为主持人资料对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | number | 用户 ID |
| name | string | 姓名/昵称 |
| phone | string | 手机号 |
| contactVisible | string | 联系方式展示方式：PUBLIC（公开）、MASKED（脱敏） |
| memberEnabled | boolean | 会员是否已启用 |
| packageName | string | 当前套餐名称，未开通时可为空 |
| remainingQuota | number | 剩余项目配额数量（可创建项目数） |
| validTo | string | 会员有效期截止时间，ISO 日期时间，未开通时为 null |
| status | string | 账号状态，如 ENABLED、DISABLED |

---

### 10.2 更新我的资料

**接口含义：** 修改主持人昵称、手机号、联系方式是否对 C 端可见等（仅主持人可调用）。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/users/me` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | string | 否 | 姓名/昵称，2～20 字符 |
| phone | string | 否 | 手机号，11 位 |
| contactVisible | string | 否 | 联系方式展示方式。可选：`PUBLIC`（公开）、`MASKED`（脱敏） |

**请求示例：**
```json
{
  "name": "主持人张三",
  "phone": "13800138000",
  "contactVisible": "MASKED"
}
```

**响应体（data 为更新后的主持人资料对象）：** 字段与「10.1 我的资料」的 data 相同。

---

## 十一、用户/主持人管理（仅超管）

以下为 **`/api/users`** 下仅超管可调用的接口，需**超管 token**（`Authorization: Bearer {{token}}`）。

### 11.1 用户/主持人列表

**接口含义：** 分页查询用户/主持人账号列表，支持按状态、配额状态、关键词筛选。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/users` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：** 无  
- **Query 参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | number | 否 | 页码，从 1 开始，默认 1 |
| size | number | 否 | 每页条数，默认 10 |
| status | string | 否 | 账号状态：`ENABLED`（启用）、`DISABLED`（停用） |
| quotaStatus | string | 否 | 配额状态：充足/不足/低余额等（以实际枚举为准） |
| keyword | string | 否 | 搜索关键词，匹配姓名或手机号 |

- **请求体：** 无  

**响应体（data 为分页对象）：** 遵循文档开头「分页响应」结构；`content` 中每项为主持人列表项，结构见下表。

**content 数组中每个元素：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 主持人用户 ID |
| name | string | 主持人姓名 |
| phone | string | 主持人手机号 |
| status | string | 账号状态：ENABLED（启用）、DISABLED（停用） |
| quotaRemaining | number | 当前剩余项目配额数量（还可创建的项目数） |
| lastLoginAt | string | 最后登录时间，ISO 日期时间，未登录过可为 null |
| createdAt | string | 账号创建时间，ISO 日期时间 |

---

### 11.2 创建主持人

**接口含义：** 超管新建一名主持人账号并可选赋予初始项目配额。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/users` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| phone | string | 是 | 手机号，1[3-9] 开头的 11 位 |
| name | string | 是 | 姓名，2～20 字符 |
| initialQuota | number | 否 | 初始项目配额数量，默认 0 |

**请求示例：**
```json
{
  "phone": "13900139000",
  "name": "新主持人",
  "initialQuota": 10
}
```

**响应体（data 为主持人详情对象）：** 字段与「11.3 主持人详情」的 data 相同，见该节。

---

### 11.3 用户/主持人详情

**接口含义：** 根据用户 ID 查询其详情及配额信息。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/users/{id}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 用户/主持人 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为用户/主持人详情对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 主持人用户 ID |
| phone | string | 手机号 |
| name | string | 姓名 |
| status | string | 账号状态：ENABLED、DISABLED |
| quotaRemaining | number | 当前剩余项目配额数量 |
| quotaTotalUsed | number | 累计已使用的项目配额数量（历史创建项目数） |
| lastLoginAt | string | 最后登录时间，ISO 日期时间 |
| createdAt | string | 账号创建时间，ISO 日期时间 |

---

### 11.4 更新用户状态

**接口含义：** 启用或停用用户/主持人账号。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/users/{id}/status` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 用户/主持人 ID |

- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | string | 是 | 状态：`ENABLED`（启用）、`DISABLED`（停用） |

**请求示例：**
```json
{
  "status": "ENABLED"
}
```

**响应体：** 成功时 `code` 为 200，`data` 为 null。

---

### 11.5 开通会员

**接口含义：** 为指定用户/主持人开通会员套餐，按套餐配置增加项目配额与有效期。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/users/{id}/activate-member` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 用户/主持人 ID |

- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| packageCode | string | 是 | 套餐编码，从系统配置 member.packages 读取，如 `annual`（年费）、`single` 等，长度 1～32 |

**请求示例：**
```json
{
  "packageCode": "annual"
}
```

**响应体：** 成功时 `code` 为 200，`data` 为 null。

---

### 11.6 调整配额

**接口含义：** 对用户/主持人项目配额做增减（正数增加、负数扣减），并记录原因。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/users/{id}/quota` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 用户/主持人 ID |

- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| delta | number | 是 | 变动量，正数增加、负数减少 |
| reason | string | 否 | 变动原因，便于流水记录 |

**请求示例：**
```json
{
  "delta": 10,
  "reason": "活动赠送"
}
```

**响应体：** 成功时 `code` 为 200，`data` 为 null。

---

### 11.7 配额流水

**接口含义：** 分页查询指定用户/主持人的项目配额变动流水（开通、调整、扣减等）。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/users/{id}/quota-flows` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 用户/主持人 ID |

- **Query 参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | number | 否 | 页码，默认 1 |
| size | number | 否 | 每页条数，默认 20 |

- **请求体：** 无  

**响应体（data 为分页对象）：** 遵循「分页响应」结构；`content` 中每项为一条配额流水，结构见下表。

**content 数组中每个元素：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 流水记录 ID |
| flowType | string | 流水类型（如开通、调整、扣减等） |
| delta | number | 本笔变动量，正数为增加、负数为扣减 |
| balanceAfter | number | 变动后的剩余配额余额 |
| reason | string | 变动原因说明 |
| refType | string | 关联业务类型（如会员、手动调整等） |
| refId | string | 关联业务 ID |
| createdAt | string | 流水发生时间，ISO 日期时间 |

---

## 十二、超管 - 项目管理（与第七章共用路径）

超管使用**与第七章相同的路径** **`/api/projects`**：  
- **项目列表**：`GET /api/projects`，传 `hostUserId`、`status`、`keyword` 等筛选，返回含 hostName、hostPhone 的分页列表。  
- **项目详情**：`GET /api/projects/{id}`，返回含 hostUserId、hostName、hostPhone、participants、contents 的详情。  
- **分享入口**：`GET /api/projects/{id}/share`，超管可查任意项目。  

请求/响应结构见**第七章**；超管与主持人仅数据范围与返回字段（如是否含 host 信息）不同。

---

## 十三、板块元数据（主持人 / 超管共用）

统一路径为 **`/api/board-meta`**。主持人（HOST）：`GET` 仅返回已启用列表；超管（SUPER_ADMIN）：`GET` 返回全部，且可 `POST`/`PUT`/`DELETE` 增删改。

### 13.1 板块元数据列表

**接口含义：** 查询板块元数据。主持人仅返回已启用；超管返回全部。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/board-meta` |
| 认证 | 是（主持人或超管 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为板块元数据对象数组）：** 每项结构同「13.2 板块元数据详情」的 data。

---

### 13.2 板块元数据详情

**接口含义：** 根据 ID 查询单条板块元数据详情。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/board-meta/{id}` |
| 认证 | 是（主持人或超管 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 板块元数据 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为板块元数据对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 板块元数据 ID |
| code | string | 板块编码，全局唯一 |
| name | string | 板块名称 |
| displayOrder | number | 展示顺序，数值越小越靠前 |
| description | string | 描述说明 |
| status | string | 状态：ENABLED、DISABLED |
| createdAt | string | 创建时间，ISO 日期时间 |
| updatedAt | string | 更新时间，ISO 日期时间 |

---

### 13.3 创建板块元数据（仅超管）

**接口含义：** 新增一种板块类型（编码、名称、顺序、描述、状态）。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/board-meta` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| code | string | 是 | 编码，最长 32 字符，如 wedding_intro |
| name | string | 是 | 名称，最长 50 字符 |
| displayOrder | number | 否 | 展示顺序，默认 0 |
| description | string | 否 | 描述，最长 200 字符 |
| status | string | 否 | 状态，默认 `ENABLED`，可选 `DISABLED` 等 |

**请求示例：**
```json
{
  "code": "wedding_intro",
  "name": "婚礼介绍",
  "displayOrder": 0,
  "description": "婚礼开场介绍",
  "status": "ENABLED"
}
```

**响应体（data 为新建的板块元数据对象）：** 字段与「13.2 板块元数据详情」的 data 相同。

---

### 13.4 更新板块元数据（仅超管）

**接口含义：** 修改板块元数据的名称、顺序、描述、状态。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/board-meta/{id}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 板块元数据 ID |

- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | string | 否 | 名称，最长 50 字符 |
| displayOrder | number | 否 | 展示顺序 |
| description | string | 否 | 描述，最长 200 字符 |
| status | string | 否 | 状态 |

**请求示例：**
```json
{
  "name": "婚礼介绍(更新)",
  "displayOrder": 1,
  "description": "更新描述",
  "status": "ENABLED"
}
```

**响应体（data 为更新后的板块元数据对象）：** 字段与「13.2 板块元数据详情」的 data 相同。

---

### 13.5 删除板块元数据（仅超管）

**接口含义：** 删除指定板块元数据（可能受是否存在关联项目板块限制）。

| 项目 | 说明 |
|------|------|
| 方法 | `DELETE` |
| 路径 | `/api/board-meta/{id}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 板块元数据 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体：** 成功时 `code` 为 200，`data` 为 null。

---

## 十三（续）、交付物元数据（主持人 / 超管共用）

统一路径为 **`/api/deliverable-meta`**。主持人（HOST）：`GET` 仅返回已启用列表；超管（SUPER_ADMIN）：`GET` 返回全部，且可 `POST`/`PUT`/`DELETE` 增删改。用于主持人端展示「可生成的交付物类型」列表。

### 13（续）.1 交付物元数据列表

**接口含义：** 查询交付物元数据。主持人仅返回已启用；超管返回全部。按 display_order 排序。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/deliverable-meta` |
| 认证 | 是（主持人或超管 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为交付物元数据对象数组）：** 每项结构同「13（续）.3 交付物元数据详情」的 data。

---

### 13（续）.2 已启用交付物元数据列表

**接口含义：** 仅返回已启用（status=ENABLED）的交付物类型，不区分角色。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/deliverable-meta/enabled` |
| 认证 | 是（主持人或超管 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为交付物元数据对象数组）：** 每项结构同「13（续）.3 交付物元数据详情」的 data。

---

### 13（续）.3 交付物元数据详情

**接口含义：** 根据 ID 查询单条交付物元数据详情。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/deliverable-meta/{id}` |
| 认证 | 是（主持人或超管 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 交付物元数据 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为交付物元数据对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 交付物元数据 ID |
| code | string | 交付物类型编码，与生成接口 contentType 一致，如 OPENING_SPEECH、GROOM_VOW、BRIDE_VOW |
| name | string | 显示名称，如婚礼开场白、新郎誓言、新娘誓言 |
| displayOrder | number | 展示顺序，数值越小越靠前 |
| description | string | 描述说明 |
| status | string | 状态：ENABLED、DISABLED |
| createdAt | string | 创建时间，ISO 日期时间 |
| updatedAt | string | 更新时间，ISO 日期时间 |

---

### 13（续）.4 创建交付物元数据（仅超管）

**接口含义：** 新增一种可生成的交付物类型（编码需与 ContentType 枚举一致方可被生成接口使用）。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/deliverable-meta` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| code | string | 是 | 编码，最长 32 字符，如 OPENING_SPEECH |
| name | string | 是 | 名称，最长 50 字符 |
| displayOrder | number | 否 | 展示顺序，默认 0 |
| description | string | 否 | 描述，最长 200 字符 |
| status | string | 否 | 状态，默认 `ENABLED`，可选 `DISABLED` |

**请求示例：**
```json
{
  "code": "OPENING_SPEECH",
  "name": "婚礼开场白",
  "displayOrder": 1,
  "description": "根据新郎新娘已确认素材生成婚礼开场白",
  "status": "ENABLED"
}
```

**响应体（data 为新建的交付物元数据对象）：** 字段与「13（续）.3 交付物元数据详情」的 data 相同。

---

### 13（续）.5 更新交付物元数据（仅超管）

**接口含义：** 修改交付物元数据的名称、顺序、描述、状态。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/deliverable-meta/{id}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 交付物元数据 ID |

- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | string | 否 | 名称，最长 50 字符 |
| displayOrder | number | 否 | 展示顺序 |
| description | string | 否 | 描述，最长 200 字符 |
| status | string | 否 | 状态 |

**响应体（data 为更新后的交付物元数据对象）：** 字段与「13（续）.3 交付物元数据详情」的 data 相同。

---

### 13（续）.6 删除交付物元数据（仅超管）

**接口含义：** 删除指定交付物元数据。

| 项目 | 说明 |
|------|------|
| 方法 | `DELETE` |
| 路径 | `/api/deliverable-meta/{id}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 交付物元数据 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体：** 成功时 `code` 为 200，`data` 为 null。

---

## 十四、超管 - 仪表盘（系统总览）

### 14.1 仪表盘总览

**接口含义：** 获取 P01 系统总览页面数据，包含今日指标、成本估算、最近项目、总用户数、待处理内容等。仅 SUPER_ADMIN 可访问。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/admin/dashboard` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为 DashboardOverviewResponse）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| todayMetrics | object | 今日关键指标 |
| todayMetrics.todayProjects | number | 今日新增项目数 |
| todayMetrics.todayActiveSessions | number | 今日采访会话数（活跃） |
| todayMetrics.todayGenerations | number | 今日生成次数 |
| todayMetrics.failureRatePercent | number | 失败率（0-100），首期暂无返回 0 |
| costMetrics | object | 成本估算（今日） |
| costMetrics.speechMinutes | number | 语音识别分钟数 |
| costMetrics.modelCallUsage | number | 模型调用消耗量，首期暂无返回 0 |
| costMetrics.storageObjectCount | number | 存储对象数（近似口径） |
| recentFailures | array | 最近失败记录，首期暂无返回 [] |
| recentProjects | array | 最近项目列表（最多 10 条） |
| recentProjects[].id | number | 项目 ID |
| recentProjects[].projectNo | string | 项目编号 |
| recentProjects[].title | string | 项目标题（新人名组合） |
| recentProjects[].status | string | 项目状态 |
| recentProjects[].createdAt | string | 创建时间 |
| totalUsers | number | 总用户数 |
| pendingContentCount | number | 待处理内容数（OUTDATED 状态） |

---

## 十四（续）、超管 - 提示词供给（按场景）

### 14.2 按场景获取提示词

**接口含义：** 按场景编码获取该场景下即将生效的提示词内容列表，用于调试与验收。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/admin/prompts/scene/{sceneCode}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sceneCode | string | 是 | 场景编码，如 WEDDING_INTRO |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为提示词内容对象数组）：** 每项结构见下表。

| 字段 | 类型 | 说明 |
|------|------|------|
| promptCode | string | 提示词编码 |
| versionNo | number | 使用的版本号 |
| content | string | 该条提示词正文内容 |
| displayOrder | number | 在场景中的展示/拼接顺序 |
| usageMode | string | 使用方式，如 APPEND（追加） |

---

### 14.3 按场景获取合并内容

**接口含义：** 按场景编码获取合并后的完整提示词字符串（即实际发给大模型的内容预览）。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/admin/prompts/scene/{sceneCode}/combined` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sceneCode | string | 是 | 场景编码 |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为字符串）：** `data` 为按场景合并后的完整提示词文本（即实际发给大模型的整段内容），无嵌套字段。

---

## 十五、超管 - 提示词管理

### 15.1 提示词列表

**接口含义：** 查询当前生效的提示词列表，支持按内容类型、状态筛选。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/admin/prompts` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：** 无  
- **Query 参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| contentType | string | 否 | 内容类型，如 TEXT |
| status | string | 否 | 状态，如 ENABLED、DISABLED |

- **请求体：** 无  

**响应体（data 为提示词对象数组）：** 每项结构同「15.2 提示词详情」的 data，见该节。

---

### 15.2 提示词详情

**接口含义：** 根据提示词 ID 查询详情（含当前生效版本信息）。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/admin/prompts/{id}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 提示词 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为提示词对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 提示词 ID |
| code | string | 提示词编码 |
| name | string | 提示词名称 |
| contentType | string | 内容类型，如 TEXT |
| description | string | 描述 |
| status | string | 状态：ENABLED、DISABLED |
| versionNo | number | 当前行版本号 |
| content | string | 当前行正文内容 |
| isActive | boolean | 是否当前生效 |
| createdAt | string | 创建时间，ISO 日期时间 |
| createdBy | number | 创建人用户 ID |
| updatedAt | string | 更新时间，ISO 日期时间 |

---

### 15.3 创建提示词

**接口含义：** 新建一条提示词（编码、名称、正文等，首版 version_no=1 且生效）。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/admin/prompts` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}`、`Content-Type: application/json` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| code | string | 是 | 编码，最长 64 字符 |
| name | string | 是 | 名称，最长 100 字符 |
| contentType | string | 否 | 内容类型，默认 TEXT |
| description | string | 否 | 描述，最长 500 字符 |
| status | string | 否 | 状态，默认 ENABLED |
| content | string | 是 | 提示词正文（首版内容） |

**请求示例：**
```json
{
  "code": "opening_prompt",
  "name": "开场白提示词",
  "contentType": "TEXT",
  "description": "婚礼开场白",
  "status": "ENABLED",
  "content": "你的核心能力是：深度解构新人碎片的采访素材..."
}
```

**响应体（data 为新建的提示词对象）：** 字段与「15.2 提示词详情」的 data 相同。

---

### 15.4 更新提示词

**接口含义：** 修改提示词名称、内容类型、描述、状态或正文（按 id 更新该行）。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/admin/prompts/{id}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 提示词 ID |

- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | string | 否 | 名称，最长 100 字符 |
| contentType | string | 否 | 内容类型 |
| description | string | 否 | 描述，最长 500 字符 |
| status | string | 否 | 状态 |
| content | string | 否 | 提示词正文 |

**请求示例：**
```json
{
  "name": "开场白提示词(更新)",
  "contentType": "TEXT",
  "description": "更新描述",
  "status": "ENABLED"
}
```

**响应体（data 为更新后的提示词对象）：** 字段与「15.2 提示词详情」的 data 相同。

---

### 15.5 删除提示词

**接口含义：** 删除指定提示词（按 id 删除该行；若该 code 被场景引用则禁止）。

| 项目 | 说明 |
|------|------|
| 方法 | `DELETE` |
| 路径 | `/api/admin/prompts/{id}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 提示词 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体：** 成功时 `code` 为 200，`data` 为 null。

---

## 十六、超管 - 提示词版本

### 16.1 版本列表

**接口含义：** 查询某提示词（按 code）下的所有版本（历史与当前生效）。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/admin/prompts/{code}/versions` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| code | string | 是 | 提示词编码 |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为版本对象数组）：** 每项结构同「16.2 版本详情」的 data，见该节。

---

### 16.2 版本详情

**接口含义：** 根据版本 ID 查询单条版本详情。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/admin/prompts/{code}/versions/{versionId}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| code | string | 是 | 提示词编码 |
| versionId | number | 是 | 版本 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为提示词版本对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 版本 ID |
| code | string | 所属提示词编码 |
| versionNo | number | 版本号（从 1 递增） |
| content | string | 该版本正文内容 |
| isActive | boolean | 是否为当前生效版本 |
| createdAt | string | 创建时间，ISO 日期时间 |
| createdBy | number | 创建人用户 ID |

---

### 16.3 创建版本

**接口含义：** 为提示词（按 code）新增一个版本内容，并可选择是否设为当前生效版本。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/admin/prompts/{code}/versions` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| code | string | 是 | 提示词编码 |

- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| content | string | 是 | 版本正文内容 |
| setActive | boolean | 否 | 是否设为当前生效版本，默认 true |

**请求示例：**
```json
{
  "content": "版本内容",
  "setActive": true
}
```

**响应体（data 为新建的版本对象）：** 字段与「16.2 版本详情」的 data 相同。

---

### 16.4 激活版本

**接口含义：** 将指定版本设为该提示词的当前生效版本（用于生成/预览时使用）。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/admin/prompts/{code}/versions/{versionId}/activate` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| code | string | 是 | 提示词编码 |
| versionId | number | 是 | 版本 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为更新后的版本对象）：** 字段与「16.2 版本详情」的 data 相同。

---

### 16.5 删除版本

**接口含义：** 删除指定版本（当前生效版本不可删）。

| 项目 | 说明 |
|------|------|
| 方法 | `DELETE` |
| 路径 | `/api/admin/prompts/{code}/versions/{versionId}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| code | string | 是 | 提示词编码 |
| versionId | number | 是 | 版本 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体：** 成功时 `code` 为 200，`data` 为 null。

---

## 十七、超管 - 提示词场景

### 17.1 场景列表

**接口含义：** 查询提示词场景列表，支持按作用域、板块编码、角色类型、状态筛选。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/admin/prompt-scenes` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：** 无  
- **Query 参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| scope | string | 否 | 作用域，如 GLOBAL |
| boardCode | string | 否 | 板块编码 |
| roleType | string | 否 | 角色类型 |
| status | string | 否 | 状态，如 ENABLED、DISABLED |

- **请求体：** 无  

**响应体（data 为场景对象数组）：** 每项结构同「17.2 场景详情」的 data（含 items 列表），见该节。

---

### 17.2 场景详情

**接口含义：** 根据场景 ID 查询场景详情及关联的提示词条目。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/admin/prompt-scenes/{sceneId}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sceneId | number | 是 | 场景 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为提示词场景对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 场景 ID |
| code | string | 场景编码 |
| name | string | 场景名称 |
| scope | string | 作用域，如 GLOBAL |
| boardCode | string | 关联板块编码，可为 null |
| roleType | string | 角色类型，可为 null |
| description | string | 描述 |
| status | string | 状态：ENABLED、DISABLED |
| createdAt | string | 创建时间，ISO 日期时间 |
| updatedAt | string | 更新时间，ISO 日期时间 |
| items | array | 场景下绑定的提示词项列表，每项结构见下表 |

**items 数组中每个元素：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 场景项 ID（场景与提示词关联记录 ID） |
| sceneId | number | 所属场景 ID |
| promptCode | string | 关联的提示词编码 |
| promptName | string | 提示词名称 |
| displayOrder | number | 展示/拼接顺序 |
| usageMode | string | 使用方式，如 APPEND |

---

### 17.3 创建场景

**接口含义：** 新建一个提示词场景（编码、名称、作用域、可选板块/角色、描述、状态）。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/admin/prompt-scenes` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}`、`Content-Type: application/json` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| code | string | 是 | 场景编码，最长 64 字符，如 wedding_intro |
| name | string | 是 | 名称，最长 100 字符 |
| scope | string | 是 | 适用范围，如 GLOBAL |
| boardCode | string | 否 | 关联板块编码，最长 32 字符 |
| roleType | string | 否 | 角色类型 |
| description | string | 否 | 描述，最长 500 字符 |
| status | string | 否 | 状态，默认 ENABLED |

**请求示例：**
```json
{
  "code": "wedding_intro",
  "name": "婚礼介绍",
  "scope": "GLOBAL",
  "boardCode": null,
  "roleType": null,
  "description": "婚礼介绍场景",
  "status": "ENABLED"
}
```

**响应体（data 为新建的场景对象）：** 字段与「17.2 场景详情」的 data 相同。

---

### 17.4 创建场景并绑定首条提示词

**接口含义：** 一次请求完成：新建场景 + 新建一条提示词（首版，version_no=1）+ 将该提示词绑定到该场景。适用于「新增场景时顺带带一条提示词」的流程；返回带条目的场景详情。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/admin/prompt-scenes/with-first-prompt` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}`、`Content-Type: application/json` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体（JSON）：** 包含场景字段 + 首条提示词字段 + 该条在场景中的顺序与用法。

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| code | string | 是 | 场景编码，最长 64 字符 |
| name | string | 是 | 场景名称，最长 100 字符 |
| scope | string | 是 | 适用范围，如 GLOBAL |
| boardCode | string | 否 | 关联板块编码，最长 32 字符 |
| roleType | string | 否 | 角色类型 |
| description | string | 否 | 场景描述，最长 500 字符 |
| status | string | 否 | 场景状态，默认 ENABLED |
| firstPromptCode | string | 是 | 首条提示词编码，最长 64 字符（全局唯一，不可与已有提示词重复） |
| firstPromptName | string | 是 | 首条提示词名称，最长 100 字符 |
| firstPromptContent | string | 是 | 首条提示词正文 |
| firstPromptContentType | string | 否 | 首条提示词内容类型，默认 TEXT |
| firstPromptDescription | string | 否 | 首条提示词描述，最长 500 字符 |
| firstPromptStatus | string | 否 | 首条提示词状态，默认 ENABLED |
| firstPromptDisplayOrder | number | 否 | 该条在场景中的展示/拼接顺序，默认 0 |
| firstPromptUsageMode | string | 否 | 该条使用方式，如 APPEND，默认 APPEND |

**请求示例：**
```json
{
  "code": "MY_NEW_SCENE",
  "name": "我的新场景",
  "scope": "GLOBAL",
  "boardCode": null,
  "roleType": null,
  "description": "新场景说明",
  "status": "ENABLED",
  "firstPromptCode": "MY_NEW_PROMPT",
  "firstPromptName": "新场景首条提示词",
  "firstPromptContent": "你是一个温暖的婚礼助手...",
  "firstPromptContentType": "TEXT",
  "firstPromptDescription": null,
  "firstPromptStatus": "ENABLED",
  "firstPromptDisplayOrder": 0,
  "firstPromptUsageMode": "APPEND"
}
```

**响应体（data 为提示词场景对象）：** 与「17.2 场景详情」相同，且 `items` 中已包含刚绑定的首条提示词。

**错误：** 若场景编码已存在或首条提示词编码已存在，返回 400。

---

### 17.5 更新场景

**接口含义：** 修改场景的名称、板块编码、角色类型、描述、状态。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/admin/prompt-scenes/{sceneId}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sceneId | number | 是 | 场景 ID |

- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | string | 否 | 名称，最长 100 字符 |
| boardCode | string | 否 | 板块编码，最长 32 字符 |
| roleType | string | 否 | 角色类型 |
| description | string | 否 | 描述，最长 500 字符 |
| status | string | 否 | 状态 |

**请求示例：**
```json
{
  "name": "婚礼介绍(更新)",
  "boardCode": null,
  "roleType": null,
  "description": "更新描述",
  "status": "ENABLED"
}
```

**响应体（data 为更新后的场景对象）：** 字段与「17.2 场景详情」的 data 相同。

---

### 17.6 删除场景

**接口含义：** 删除指定提示词场景。

| 项目 | 说明 |
|------|------|
| 方法 | `DELETE` |
| 路径 | `/api/admin/prompt-scenes/{sceneId}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sceneId | number | 是 | 场景 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体：** 成功时 `code` 为 200，`data` 为 null。

---

### 17.7 添加场景项

**接口含义：** 在场景中关联一条提示词并设置顺序与使用方式（如追加/替换）。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/admin/prompt-scenes/{sceneId}/items` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sceneId | number | 是 | 场景 ID |

- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| promptCode | string | 是 | 提示词编码 |
| displayOrder | number | 否 | 展示/拼接顺序，默认 0 |
| usageMode | string | 否 | 使用方式，默认 APPEND（追加），可选其他（以实际枚举为准） |

**请求示例：**
```json
{
  "promptCode": "WEDDING_INTRO",
  "displayOrder": 0,
  "usageMode": "APPEND"
}
```

**响应体（data 为场景项对象）：** 字段与「17.2 场景详情」中 items 数组元素结构相同（id、sceneId、promptCode、promptName、displayOrder、usageMode）。

---

### 17.8 更新场景项

**接口含义：** 修改场景项的展示顺序或使用方式。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/admin/prompt-scenes/items/{itemId}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| itemId | number | 是 | 场景项 ID（场景与提示词关联记录的 ID） |

- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| displayOrder | number | 否 | 展示顺序 |
| usageMode | string | 否 | 使用方式，如 APPEND |

**请求示例：**
```json
{
  "displayOrder": 1,
  "usageMode": "APPEND"
}
```

**响应体（data 为更新后的场景项对象）：** 字段与「17.2 场景详情」中 items 数组元素结构相同。

---

### 17.9 删除场景项

**接口含义：** 从场景中移除一条提示词关联（删除场景项）。

| 项目 | 说明 |
|------|------|
| 方法 | `DELETE` |
| 路径 | `/api/admin/prompt-scenes/items/{itemId}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| itemId | number | 是 | 场景项 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体：** 成功时 `code` 为 200，`data` 为 null。

---

---

## 文档修订说明

- 认证、项目、板块元数据、用户、交付物已按「合并主持人与管理员接口」路线统一路径，不再按 `/api/b/*` 与 `/api/admin/*` 区分主持人/超管，改为**同一路径 + 角色鉴权**。
- 旧路径（如 `/api/b/projects`、`/api/admin/hosts`）已废弃，请使用新路径（如 `/api/projects`、`/api/users`）。
- 超管专属能力（提示词、语音配额等）仍保留在 `/api/admin/*`。
- **认证**：`POST /api/auth/wechat-login` 微信小程序登录（前端传 `code`，后端 code2session 换 openid 后查/建用户并签发 token）；补充 `POST /api/auth/login-password`、`POST /api/auth/set-password` 说明。
- **C 端入口**：`GET /api/c/entry/{token}` 响应增加 `theme`、`roleOptions`（后端返回可选角色及是否可点选），选择身份页由后端驱动。
- **C 端绑定与会话合并**：原 `POST /api/c/projects/{projectId}/bind` 已移除，与「创建/恢复会话」合并为 **`POST /api/c/sessions`**。**会话按板块**：body 必填 `projectId` + `projectBoardId`；未绑定时还需 `role`（GROOM/BRIDE）。响应 data 为会话对象（含 `role` 字段）。换板块即新 session。
- **C 端流式提交**：新增 **WebSocket** `ws(s)://{host}/api/c/ws/submit-stream?token={JWT}`。连接后发送 `{"sessionId": number}` 即触发展开，服务端按 DeepSeek 流式返回 delta，最后推送 done（含 messageId、roundCount）或 error。详见「4.10 流式提交（WebSocket）」。

---

## 接口索引（按路径）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 根路径/存活 |
| GET | `/api/health` | 健康检查 |
| POST | `/api/auth/send-code` | 发送验证码 |
| POST | `/api/auth/login` | 登录（手机+验证码） |
| POST | `/api/auth/login-password` | 密码登录 |
| POST | `/api/auth/wechat-login` | 微信小程序登录 |
| POST | `/api/auth/set-password` | 设置/修改密码 |
| POST | `/api/auth/logout` | 退出 |
| GET | `/api/auth/me` | 当前用户 |
| GET | `/api/c/entry/{token}` | C 端入口-项目信息（含 roleOptions） |
| GET | `/api/c/speech-quota` | C 端语音转写配额 |
| WS | `ws(s)://{host}/api/c/ws/speech-recognition` | C 端实时语音转写（WebSocket） |
| POST | `/api/c/sessions` | C 端进入某板块采访（body 必填 projectId + projectBoardId，未绑定时必填 role） |
| GET | `/api/c/sessions/{sessionId}` | C 端会话详情 |
| GET | `/api/c/sessions/{sessionId}/messages` | C 端消息列表 |
| POST | `/api/c/sessions/{sessionId}/messages` | C 端发送消息 |
| PUT | `/api/c/sessions/{sessionId}/messages/{messageId}` | C 端更新消息 |
| DELETE | `/api/c/sessions/{sessionId}/messages/{messageId}` | C 端删除消息 |
| POST | `/api/c/sessions/{sessionId}/submit` | C 端提交给 AI（一次性返回） |
| WS | `/api/c/ws/submit-stream` | C 端流式提交（WebSocket，提交后流式收 AI 回复） |
| POST | `/api/c/sessions/{sessionId}/summaries` | C 端创建小结 |
| GET | `/api/c/sessions/{sessionId}/summaries/current` | C 端当前小结 |
| GET | `/api/c/board-summaries/{summaryId}` | C 端小结详情 |
| PUT | `/api/c/summary-items/{itemId}` | C 端更新小结条目 |
| POST | `/api/c/board-summaries/{summaryId}/items` | C 端新增小结条目 |
| DELETE | `/api/c/summary-items/{itemId}` | C 端删除小结条目 |
| PUT | `/api/c/board-summaries/{summaryId}/key-persons` | C 端设置小结关键人物 |
| GET | `/api/c/users/me/boards` | C 端我的板块列表（有故事的板块，用于按板块 Tab/筛选故事） |
| GET | `/api/c/users/me/stories` | C 端我的故事列表（支持按会话/板块/角色筛选） |
| POST | `/api/c/sessions/{sessionId}/stories` | C 端创建故事 |
| GET | `/api/c/sessions/{sessionId}/stories` | C 端按会话+板块获取故事（单条） |
| GET | `/api/c/sessions/{sessionId}/persons` | C 端人物列表 |
| POST | `/api/c/sessions/{sessionId}/persons` | C 端新增人物 |
| PUT | `/api/c/key-persons/{personId}` | C 端更新关键人物 |
| DELETE | `/api/c/key-persons/{personId}` | C 端删除关键人物 |
| GET | `/api/projects` | 项目列表 |
| POST | `/api/projects` | 创建项目 |
| GET | `/api/projects/{id}` | 项目详情 |
| GET | `/api/projects/{id}/share` | 分享入口 |
| POST | `/api/projects/{projectId}/deliverables/generate` | 生成交付物 |
| GET | `/api/projects/{projectId}/deliverables/{contentType}` | 按类型获取交付物 |
| GET | `/api/deliverables/{id}` | 交付物详情 |
| PUT | `/api/deliverables/{id}` | 更新交付物 |
| DELETE | `/api/deliverables/{id}` | 删除交付物 |
| GET | `/api/board-meta` | 板块元数据列表 |
| GET | `/api/board-meta/enabled` | 已启用板块列表 |
| GET | `/api/board-meta/{id}` | 板块元数据详情 |
| POST | `/api/board-meta` | 创建板块元数据（超管） |
| PUT | `/api/board-meta/{id}` | 更新板块元数据（超管） |
| DELETE | `/api/board-meta/{id}` | 删除板块元数据（超管） |
| GET | `/api/deliverable-meta` | 交付物元数据列表 |
| GET | `/api/deliverable-meta/enabled` | 已启用交付物元数据列表 |
| GET | `/api/deliverable-meta/{id}` | 交付物元数据详情 |
| POST | `/api/deliverable-meta` | 创建交付物元数据（超管） |
| PUT | `/api/deliverable-meta/{id}` | 更新交付物元数据（超管） |
| DELETE | `/api/deliverable-meta/{id}` | 删除交付物元数据（超管） |
| GET | `/api/projects/{projectId}/boards` | 项目板块列表 |
| POST | `/api/projects/{projectId}/boards` | 添加项目板块 |
| PUT | `/api/projects/{projectId}/boards/{projectBoardId}` | 更新项目板块 |
| DELETE | `/api/projects/{projectId}/boards/{projectBoardId}` | 删除项目板块 |
| GET | `/api/users/me` | 我的资料 |
| PUT | `/api/users/me` | 更新我的资料 |
| GET | `/api/users` | 用户列表（超管） |
| POST | `/api/users` | 创建主持人（超管） |
| GET | `/api/users/{id}` | 用户详情（超管） |
| PUT | `/api/users/{id}/status` | 更新用户状态（超管） |
| POST | `/api/users/{id}/activate-member` | 开通/续费会员（超管） |
| PUT | `/api/users/{id}/quota` | 调整配额（超管） |
| GET | `/api/users/{id}/quota-flows` | 配额流水（超管） |
| DELETE | `/api/users/{id}` | 删除用户（超管） |
| GET | `/api/admin/dashboard` | 超管仪表盘 |
| GET | `/api/admin/deliverables` | 超管交付物列表 |
| GET | `/api/admin/prompts` | 提示词列表 |
| GET | `/api/admin/prompts/{id}` | 提示词详情 |
| POST | `/api/admin/prompts` | 创建提示词 |
| PUT | `/api/admin/prompts/{id}` | 更新提示词 |
| DELETE | `/api/admin/prompts/{id}` | 删除提示词 |
| GET | `/api/admin/prompts/scene/{sceneCode}` | 按场景获取提示词 |
| GET | `/api/admin/prompts/scene/{sceneCode}/combined` | 按场景获取合并内容 |
| GET | `/api/admin/prompts/{code}/versions` | 版本列表 |
| GET | `/api/admin/prompts/{code}/versions/{versionId}` | 版本详情 |
| POST | `/api/admin/prompts/{code}/versions` | 创建版本 |
| PUT | `/api/admin/prompts/{code}/versions/{versionId}/activate` | 激活版本 |
| DELETE | `/api/admin/prompts/{code}/versions/{versionId}` | 删除版本 |
| GET | `/api/admin/prompt-scenes` | 场景列表 |
| GET | `/api/admin/prompt-scenes/{id}` | 场景详情 |
| POST | `/api/admin/prompt-scenes` | 创建场景 |
| POST | `/api/admin/prompt-scenes/with-first-prompt` | 创建场景并绑定首条提示词 |
| PUT | `/api/admin/prompt-scenes/{id}` | 更新场景 |
| DELETE | `/api/admin/prompt-scenes/{id}` | 删除场景 |
| POST | `/api/admin/prompt-scenes/{id}/items` | 添加场景项 |
| PUT | `/api/admin/prompt-scenes/items/{itemId}` | 更新场景项 |
| DELETE | `/api/admin/prompt-scenes/items/{itemId}` | 删除场景项 |
| GET | `/api/admin/config` | 查询全部配置项（sys_config） |
| PUT | `/api/admin/config/{configKey}` | 按 configKey 修改配置项 |
| GET | `/api/admin/config/password-policy` | 获取密码策略 |
| PUT | `/api/admin/config/password-policy` | 更新密码策略 |
| POST | `/api/admin/speech-quota/add` | 超管增加用户语音配额 |

*文档基于项目 Controller/DTO 整理；若接口有变更请以实际代码为准。*
