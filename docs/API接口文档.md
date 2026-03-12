# 数字爸爸 API 接口文档

数字爸爸 - 婚礼采访与生成平台接口说明。

**使用说明：**
1. 先调用「认证 - 发送验证码」获取验证码。
2. 调用「认证 - 主持人登录」或「认证 - 超管登录」获取 token。
3. 需认证的接口在请求头中携带：`Authorization: Bearer <token>`。

**基础地址：** `{{baseUrl}}`（示例：`http://101.34.64.224:8080`）

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

### 2.2 主持人登录

**接口含义：** 主持人使用手机号 + 验证码登录，返回访问令牌（token），用于后续主持人端与 C 端接口鉴权。

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

**请求示例：**
```json
{
  "phone": "13800138000",
  "code": "123456"
}
```

**响应体（data 为登录结果对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| token | string | JWT 访问令牌，后续请求需在 Header 中携带 `Authorization: Bearer <token>` |
| userId | number | 当前登录用户的用户 ID |
| userType | string | 用户类型，如 HOST（主持人） |
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
    "name": "主持人昵称",
    "phone": "13800138000"
  }
}
```

---

### 2.3 主持人 - 当前用户

**接口含义：** 根据当前 token 获取主持人当前登录用户信息。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/auth/me` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为当前用户信息对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | number | 当前登录用户的用户 ID |
| userType | string | 用户类型，如 HOST（主持人） |
| name | string | 用户姓名/昵称 |
| phone | string | 用户手机号 |
| avatarUrl | string | 用户头像 URL，可能为空 |

---

### 2.4 主持人 - 退出登录

**接口含义：** 主持人主动退出登录，使当前 token 失效（若服务端实现 token 黑名单）。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/auth/logout` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体：** 无  

**响应体：** 成功时 `code` 为 200，`data` 为 null。

---

### 2.5 超管登录

**接口含义：** 超管使用手机号 + 验证码登录，返回超管专用 token，用于所有超管后台接口。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/admin/auth/login` |
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

**请求示例：**
```json
{
  "phone": "13800138000",
  "code": "123456"
}
```

**响应体（data 结构与「主持人登录」一致）：** `data` 中 `token` 用作超管接口的 `Authorization: Bearer {{adminToken}}`；`userType` 为超管类型。字段含义同 2.2 主持人登录的 data 表格。

---

### 2.6 超管 - 当前用户

**接口含义：** 根据当前超管 token 获取超管当前登录用户信息。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/admin/auth/me` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为当前超管用户信息对象）：** 字段与「2.3 主持人 - 当前用户」的 data 相同（userId、userType、name、phone、avatarUrl），其中 userType 为超管类型。

---

## 三、C 端 - 入口（免认证）

### 3.1 扫码入口 - 获取项目信息

**接口含义：** 用户通过分享链接/扫码进入时，凭分享 token 获取项目基本信息（如婚礼名称、新人姓名等），无需登录。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/c/entry/{shareToken}` |
| 认证 | 否 |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| shareToken | string | 是 | 项目分享 token，由「获取分享入口」接口得到 |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为项目信息对象，供 C 端身份选择页展示）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| projectId | number | 项目 ID |
| shareToken | string | 当前项目对应的分享令牌（与入口 URL 中一致） |
| groomName | string | 新郎姓名 |
| brideName | string | 新娘姓名 |
| weddingDate | string | 婚期，日期格式如 "2025-06-01" |

---

## 四、C 端 - 绑定与会话

以下接口均需**主持人 token**（即主持人登录后获得的 token，用于代表“当前访问者”的会话与权限）。

### 4.1 绑定参与者

**接口含义：** 用户进入项目后选择身份（新郎/新娘等），绑定为该项目的一名参与者，后续会话与该参与者关联。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/c/projects/{projectId}/bind` |
| 认证 | 是（主持人 token） |
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
| role | string | 是 | 角色标识。可选：`GROOM`（新郎）、`BRIDE`（新娘）等，以实际业务枚举为准 |

**请求示例：**
```json
{
  "role": "GROOM"
}
```

**响应体（data 为绑定结果对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| participantId | number | 本次绑定生成的参与者 ID，后续「创建/恢复会话」时必传 |
| projectId | number | 项目 ID |
| role | string | 当前绑定的角色，如 GROOM、BRIDE |
| interviewUrl | string | 采访入口 URL，绑定成功后前端可跳转至该地址继续流程 |

---

### 4.2 创建 / 恢复会话

**接口含义：** 为当前用户（已绑定参与者）创建新会话，或恢复已有未提交会话；返回会话 ID 供后续消息、小结等使用。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/c/sessions` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| participantId | number | 是 | 参与者 ID，来自「绑定参与者」返回 |

**请求示例：**
```json
{
  "participantId": 1
}
```

**响应体（data 为会话对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 会话 ID |
| projectId | number | 所属项目 ID |
| participantId | number | 所属参与者 ID |
| currentProjectBoardId | number | 当前进行中的项目板块 ID，未开始或已全部完成可为 null |
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

### 4.3 获取会话

**接口含义：** 根据会话 ID 查询会话详情及状态。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/c/sessions/{sessionId}` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | number | 是 | 会话 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为会话对象）：** 字段与「4.2 创建/恢复会话」的 data 完全相同，见上表。

---

### 4.4 获取消息列表

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
- **响应体（data 为消息对象数组）：** 按时间或序号排序，每条元素结构如下。

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

### 4.5 发送消息

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

**响应体（data 为单条消息对象）：** 字段与「4.4 获取消息列表」中数组元素结构相同。

---

### 4.6 更新消息

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

**响应体（data 为更新后的消息对象）：** 字段与「4.4 获取消息列表」中数组元素结构相同。

---

### 4.7 删除消息

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

### 4.8 提交会话

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
| roundCount | number | 提交后会话的总轮数 |
| newMessages | array | 本轮新生成的消息列表，每项结构同「4.4 获取消息列表」中的消息对象 |

---

## 五、C 端 - 小结

### 5.1 创建小结

**接口含义：** 针对当前会话创建一份小结（用于整理采访中的事实与表达），可基于会话消息自动生成初版。

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
| items | array | 小结条目列表，每项结构见下表 |

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

### 5.7 确认小结

**接口含义：** 确认当前小结为最终版，确认后可用于后续故事/交付物生成。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/c/board-summaries/{summaryId}/confirm` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| summaryId | number | 是 | 小结 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体：** 成功时 `code` 为 200，`data` 为 null。

---

## 六、C 端 - 故事与人物

### 6.1 创建故事

**接口含义：** 基于当前会话（及小结）创建或初始化“故事”数据，用于后续生成婚礼故事/文案。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/c/sessions/{sessionId}/stories` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | number | 是 | 会话 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为故事/时光对象）：** 结构同「6.2 获取故事」的 data，见该节。

---

### 6.2 获取故事

**接口含义：** 按会话与项目板块查询对应的故事内容。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/c/sessions/{sessionId}/stories` |
| 认证 | 是（主持人 token） |
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

**响应体（data 为故事对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 故事 ID |
| sessionId | number | 所属会话 ID |
| projectBoardId | number | 所属项目板块 ID |
| boardCode | string | 板块编码 |
| boardName | string | 板块名称 |
| content | string | 故事正文内容 |
| versionNo | number | 故事版本号 |
| createdAt | string | 故事创建时间，ISO 日期时间 |

---

### 6.3 获取关键人物列表

**接口含义：** 获取当前会话下已添加的关键人物列表（如新郎父亲、新娘母亲等），用于故事与文案生成。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/c/sessions/{sessionId}/persons` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | number | 是 | 会话 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为关键人物对象数组）：** 每项结构同「6.5 更新关键人物」返回的 data，见该节。

---

### 6.4 添加关键人物

**接口含义：** 在当前会话下新增一位关键人物（称谓 + 角色标签）。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/c/sessions/{sessionId}/persons` |
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
| name | string | 是 | 人物称谓，如「新郎父亲」，最长 50 字符 |
| roleLabel | string | 否 | 角色标签，如「家人」，最长 50 字符 |

**请求示例：**
```json
{
  "name": "新郎父亲",
  "roleLabel": "家人"
}
```

**响应体（data 为新增的关键人物对象）：** 字段与「6.5 更新关键人物」的 data 相同。

---

### 6.5 更新关键人物

**接口含义：** 修改已有关键人物的称谓或角色标签。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/c/key-persons/{personId}` |
| 认证 | 是（主持人 token） |
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

### 6.6 删除关键人物

**接口含义：** 删除指定关键人物。

| 项目 | 说明 |
|------|------|
| 方法 | `DELETE` |
| 路径 | `/api/c/key-persons/{personId}` |
| 认证 | 是（主持人 token） |
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

## 七、主持人 B 端 - 项目

以下接口需**主持人 token**（`Authorization: Bearer {{token}}`）。

### 7.1 创建项目

**接口含义：** 主持人创建一个新婚礼项目，填写新郎新娘姓名与婚期。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/b/projects` |
| 认证 | 是（主持人 token） |
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

**接口含义：** 分页查询当前主持人名下的项目列表。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/b/projects` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：** 无  
- **Query 参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | number | 否 | 页码，从 1 开始，默认 1 |
| size | number | 否 | 每页条数，默认 10 |

- **请求体：** 无  

**响应体（data 为分页对象）：** 遵循文档开头的「分页响应」结构；`content` 中每项为项目列表项，结构见下表。

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

**接口含义：** 根据项目 ID 查询项目详情。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/b/projects/{projectId}` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| projectId | number | 是 | 项目 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为项目详情对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 项目 ID |
| projectNo | string | 项目编号 |
| groomName | string | 新郎姓名 |
| brideName | string | 新娘姓名 |
| weddingDate | string | 婚期，日期格式 |
| status | string | 项目状态 |
| shareToken | string | 项目分享令牌 |
| createdAt | string | 项目创建时间，ISO 日期时间 |
| participants | array | 成员列表（新郎、新娘等参与者摘要），每项见下表 |
| contents | array | 生成物摘要列表，每项见下表 |

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

**contents 每项：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 生成物 ID |
| contentType | string | 类型：OPENING_SPEECH、GROOM_VOW、BRIDE_VOW |
| contentTypeName | string | 类型中文名：开场白、新郎誓言、新娘誓言 |
| title | string | 标题 |
| status | string | 状态：DRAFT、ACTIVE、OUTDATED 等 |
| versionNo | number | 版本号 |
| updatedAt | string | 更新时间，ISO 日期时间 |

---

### 7.4 获取分享入口

**接口含义：** 获取该项目的分享链接所需 token（或短链），用于 C 端「扫码入口 - 获取项目信息」。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/b/projects/{projectId}/share` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| projectId | number | 是 | 项目 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为分享入口对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| shareUrl | string | 完整分享链接（可直接复制分享） |
| entryUrl | string | C 端扫码/点击进入的入口 URL |
| qrCodeUrl | string | 二维码图片 URL，用于生成二维码供扫码 |
| shareToken | string | 分享令牌，C 端「扫码入口 - 获取项目信息」接口的路径参数即此值 |
| projectId | number | 项目 ID |

---

## 八、主持人 B 端 - 板块

### 8.1 板块列表

**接口含义：** 查询某项目下已添加的板块列表（采访主题/环节）。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/b/projects/{projectId}/boards` |
| 认证 | 是（主持人 token） |
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
| 路径 | `/api/b/projects/{projectId}/boards` |
| 认证 | 是（主持人 token） |
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
| 路径 | `/api/b/projects/{projectId}/boards/{projectBoardId}` |
| 认证 | 是（主持人 token） |
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
| 路径 | `/api/b/projects/{projectId}/boards/{projectBoardId}` |
| 认证 | 是（主持人 token） |
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

### 8.5 板块元数据 - 启用列表

**接口含义：** 获取系统中已启用的板块元数据列表，用于「添加板块」时选择 boardMetaId。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/b/board-meta/enabled` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为板块元数据对象数组）：** 每项含 id、code、name、displayOrder、description、status 等，结构同「十三、超管 - 板块元数据」中单条元数据，仅返回已启用（status=ENABLED）的元数据。

---

## 九、主持人 B 端 - 交付物

### 9.1 生成交付物

**接口含义：** 按类型为项目生成一份交付物（如婚礼开场白、新郎誓言、新娘誓言），会消耗配额并可能异步生成。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/b/projects/{projectId}/deliverables/generate` |
| 认证 | 是（主持人 token） |
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
| 路径 | `/api/b/projects/{projectId}/deliverables/{contentType}` |
| 认证 | 是（主持人 token） |
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

**接口含义：** 根据交付物 ID 查询详情。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/b/deliverables/{deliverableId}` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| deliverableId | number | 是 | 交付物 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为交付物详情对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 交付物 ID |
| projectId | number | 所属项目 ID |
| participantId | number | 关联的参与者 ID，可能为空 |
| contentType | string | 类型：OPENING_SPEECH、GROOM_VOW、BRIDE_VOW |
| contentTypeName | string | 类型中文名：婚礼开场白、新郎誓言、新娘誓言 |
| versionNo | number | 版本号 |
| title | string | 标题 |
| content | string | 正文内容 |
| status | string | 状态（如草稿、已发布等） |
| snapshotVersionAt | string | 快照版本时间，ISO 日期时间 |
| createdAt | string | 创建时间，ISO 日期时间 |
| updatedAt | string | 更新时间，ISO 日期时间 |

---

### 9.4 更新交付物

**接口含义：** 修改交付物的标题或正文内容。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/b/deliverables/{deliverableId}` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| deliverableId | number | 是 | 交付物 ID |

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
| 路径 | `/api/b/deliverables/{deliverableId}` |
| 认证 | 是（主持人 token） |
| Header | `Authorization: Bearer {{token}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| deliverableId | number | 是 | 交付物 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体：** 成功时 `code` 为 200，`data` 为 null。

---

## 十、主持人 B 端 - 用户

### 10.1 我的资料

**接口含义：** 查询当前主持人的个人资料（昵称、手机号、联系方式展示方式、配额等）。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/b/users/me` |
| 认证 | 是（主持人 token） |
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

**接口含义：** 修改主持人昵称、手机号、联系方式是否对 C 端可见等。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/b/users/me` |
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

## 十一、超管 - 主持人管理

以下接口均需**超管 token**（`Authorization: Bearer {{adminToken}}`）。

### 11.1 主持人列表

**接口含义：** 分页查询主持人账号列表，支持按状态、配额状态、关键词筛选。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/admin/hosts` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

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
| 路径 | `/api/admin/hosts` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}`、`Content-Type: application/json` |

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

### 11.3 主持人详情

**接口含义：** 根据主持人 ID 查询其详情及配额信息。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/admin/hosts/{hostId}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| hostId | number | 是 | 主持人用户 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为主持人详情对象）：**

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

### 11.4 更新主持人状态

**接口含义：** 启用或停用主持人账号。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/admin/hosts/{hostId}/status` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| hostId | number | 是 | 主持人用户 ID |

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

**接口含义：** 为指定主持人开通会员套餐，按套餐配置增加项目配额与有效期。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/admin/hosts/{hostId}/activate-member` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| hostId | number | 是 | 主持人用户 ID |

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

**接口含义：** 对主持人项目配额做增减（正数增加、负数扣减），并记录原因。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/admin/hosts/{hostId}/quota` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| hostId | number | 是 | 主持人用户 ID |

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

**接口含义：** 分页查询指定主持人的项目配额变动流水（开通、调整、扣减等）。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/admin/hosts/{hostId}/quota-flows` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| hostId | number | 是 | 主持人用户 ID |

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

## 十二、超管 - 项目管理

### 12.1 项目列表

**接口含义：** 超管分页查询全平台项目，支持按主持人、状态、关键词筛选。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/admin/projects` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：** 无  
- **Query 参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | number | 否 | 页码，默认 1 |
| size | number | 否 | 每页条数，默认 10 |
| hostUserId | number | 否 | 主持人用户 ID，不传则查全部 |
| status | string | 否 | 项目状态 |
| keyword | string | 否 | 关键词：新人姓名、项目编号等 |

- **请求体：** 无  

**响应体（data 为分页对象）：** 遵循「分页响应」结构；`content` 中每项为项目列表项（含主持人信息），结构见「12.2 项目详情」下方 content 元素说明；列表项字段为：id、projectNo、groomName、brideName、weddingDate、status、createdAt、hostUserId、hostName、hostPhone。

---

### 12.2 项目详情

**接口含义：** 超管根据项目 ID 查看项目完整详情。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/admin/projects/{projectId}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| projectId | number | 是 | 项目 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为超管视角项目详情对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 项目 ID |
| projectNo | string | 项目编号 |
| groomName | string | 新郎姓名 |
| brideName | string | 新娘姓名 |
| weddingDate | string | 婚期 |
| status | string | 项目状态 |
| shareToken | string | 分享令牌 |
| createdAt | string | 创建时间，ISO 日期时间 |
| hostUserId | number | 主持人用户 ID |
| hostName | string | 主持人姓名 |
| hostPhone | string | 主持人手机号 |
| participants | array | 参与者摘要列表，每项结构同「7.3 项目详情」的 participants 元素 |
| contents | array | 生成物摘要列表，每项结构同「7.3 项目详情」的 contents 元素 |

---

## 十三、超管 - 板块元数据

### 13.1 板块元数据列表

**接口含义：** 查询所有板块元数据（定义板块类型与名称，供项目添加板块时选择）。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/admin/board-meta` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：** 无  
- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为板块元数据对象数组）：** 每项结构同「13.2 板块元数据详情」的 data，见该节。

---

### 13.2 板块元数据详情

**接口含义：** 根据 ID 查询单条板块元数据详情。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/admin/board-meta/{id}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

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

### 13.3 创建板块元数据

**接口含义：** 新增一种板块类型（编码、名称、顺序、描述、状态）。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/admin/board-meta` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}`、`Content-Type: application/json` |

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

### 13.4 更新板块元数据

**接口含义：** 修改板块元数据的名称、顺序、描述、状态。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/admin/board-meta/{id}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}`、`Content-Type: application/json` |

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

### 13.5 删除板块元数据

**接口含义：** 删除指定板块元数据（可能受是否存在关联项目板块限制）。

| 项目 | 说明 |
|------|------|
| 方法 | `DELETE` |
| 路径 | `/api/admin/board-meta/{id}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 板块元数据 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体：** 成功时 `code` 为 200，`data` 为 null。

---

## 十四、超管 - 提示词

### 14.1 按场景获取提示词

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
| templateId | number | 提示词模板 ID |
| templateCode | string | 模板编码 |
| versionNo | number | 使用的版本号 |
| content | string | 该条提示词正文内容 |
| displayOrder | number | 在场景中的展示/拼接顺序 |
| usageMode | string | 使用方式，如 APPEND（追加） |

---

### 14.2 按场景获取合并内容

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

## 十五、超管 - 提示词模板

### 15.1 模板列表

**接口含义：** 查询提示词模板列表，支持按内容类型、状态筛选。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/admin/prompt-templates` |
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

**响应体（data 为模板对象数组）：** 每项结构同「15.2 模板详情」的 data，见该节。

---

### 15.2 模板详情

**接口含义：** 根据模板 ID 查询模板详情及当前生效版本信息。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/admin/prompt-templates/{templateId}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| templateId | number | 是 | 提示词模板 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为提示词模板对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 模板 ID |
| code | string | 模板编码 |
| name | string | 模板名称 |
| contentType | string | 内容类型，如 TEXT |
| description | string | 描述 |
| status | string | 状态：ENABLED、DISABLED |
| createdAt | string | 创建时间，ISO 日期时间 |
| updatedAt | string | 更新时间，ISO 日期时间 |
| activeVersionNo | number | 当前生效的版本号 |

---

### 15.3 创建模板

**接口含义：** 新建一个提示词模板（编码、名称、内容类型、描述、状态）。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/admin/prompt-templates` |
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

**请求示例：**
```json
{
  "code": "opening_template",
  "name": "开场白模板",
  "contentType": "TEXT",
  "description": "婚礼开场白",
  "status": "ENABLED"
}
```

**响应体（data 为新建的模板对象）：** 字段与「15.2 模板详情」的 data 相同。

---

### 15.4 更新模板

**接口含义：** 修改模板的名称、内容类型、描述、状态。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/admin/prompt-templates/{templateId}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| templateId | number | 是 | 模板 ID |

- **Query 参数：** 无  
- **请求体（JSON）：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | string | 否 | 名称，最长 100 字符 |
| contentType | string | 否 | 内容类型 |
| description | string | 否 | 描述，最长 500 字符 |
| status | string | 否 | 状态 |

**请求示例：**
```json
{
  "name": "开场白模板(更新)",
  "contentType": "TEXT",
  "description": "更新描述",
  "status": "ENABLED"
}
```

**响应体（data 为更新后的模板对象）：** 字段与「15.2 模板详情」的 data 相同。

---

### 15.5 删除模板

**接口含义：** 删除指定提示词模板（可能受场景引用限制）。

| 项目 | 说明 |
|------|------|
| 方法 | `DELETE` |
| 路径 | `/api/admin/prompt-templates/{templateId}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| templateId | number | 是 | 模板 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体：** 成功时 `code` 为 200，`data` 为 null。

---

## 十六、超管 - 提示词版本

### 16.1 版本列表

**接口含义：** 查询某模板下的所有版本（历史与当前生效）。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/admin/prompt-templates/{templateId}/versions` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| templateId | number | 是 | 模板 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为版本对象数组）：** 每项结构同「16.2 版本详情」的 data，见该节。

---

### 16.2 版本详情

**接口含义：** 根据版本 ID 查询单条版本详情。

| 项目 | 说明 |
|------|------|
| 方法 | `GET` |
| 路径 | `/api/admin/prompt-templates/{templateId}/versions/{versionId}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| templateId | number | 是 | 模板 ID |
| versionId | number | 是 | 版本 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为提示词版本对象）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 版本 ID |
| templateId | number | 所属模板 ID |
| versionNo | number | 版本号（从 1 递增） |
| content | string | 该版本正文内容 |
| isActive | boolean | 是否为当前生效版本 |
| createdAt | string | 创建时间，ISO 日期时间 |
| createdBy | number | 创建人用户 ID |

---

### 16.3 创建版本

**接口含义：** 为模板新增一个版本内容，并可选择是否设为当前生效版本。

| 项目 | 说明 |
|------|------|
| 方法 | `POST` |
| 路径 | `/api/admin/prompt-templates/{templateId}/versions` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}`、`Content-Type: application/json` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| templateId | number | 是 | 模板 ID |

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

**接口含义：** 将指定版本设为该模板的当前生效版本（用于生成/预览时使用）。

| 项目 | 说明 |
|------|------|
| 方法 | `PUT` |
| 路径 | `/api/admin/prompt-templates/{templateId}/versions/{versionId}/activate` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| templateId | number | 是 | 模板 ID |
| versionId | number | 是 | 版本 ID |

- **Query 参数：** 无  
- **请求体：** 无  

**响应体（data 为更新后的版本对象）：** 字段与「16.2 版本详情」的 data 相同。

---

### 16.5 删除版本

**接口含义：** 删除指定版本（当前生效版本可能不可删，以实际业务为准）。

| 项目 | 说明 |
|------|------|
| 方法 | `DELETE` |
| 路径 | `/api/admin/prompt-templates/{templateId}/versions/{versionId}` |
| 认证 | 是（超管 token） |
| Header | `Authorization: Bearer {{adminToken}}` |

**请求**

- **路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| templateId | number | 是 | 模板 ID |
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

**接口含义：** 根据场景 ID 查询场景详情及关联的模板条目。

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
| items | array | 场景下绑定的模板项列表，每项结构见下表 |

**items 数组中每个元素：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 场景项 ID（场景与模板关联记录 ID） |
| sceneId | number | 所属场景 ID |
| templateId | number | 关联的模板 ID |
| templateCode | string | 模板编码 |
| templateName | string | 模板名称 |
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

### 17.4 更新场景

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

### 17.5 删除场景

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

### 17.6 添加场景项

**接口含义：** 在场景中关联一个提示词模板并设置顺序与使用方式（如追加/替换）。

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
| templateId | number | 是 | 提示词模板 ID |
| displayOrder | number | 否 | 展示/拼接顺序，默认 0 |
| usageMode | string | 否 | 使用方式，默认 APPEND（追加），可选其他（以实际枚举为准） |

**请求示例：**
```json
{
  "templateId": 1,
  "displayOrder": 0,
  "usageMode": "APPEND"
}
```

**响应体（data 为场景项对象）：** 字段与「17.2 场景详情」中 items 数组元素结构相同（id、sceneId、templateId、templateCode、templateName、displayOrder、usageMode）。

---

### 17.7 更新场景项

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
| itemId | number | 是 | 场景项 ID（场景与模板关联记录的 ID） |

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

### 17.8 删除场景项

**接口含义：** 从场景中移除一条模板关联（删除场景项）。

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

*文档基于项目 Postman 集合与后端 Controller/DTO 整理；请求与响应字段均配有中文说明，若接口有变更请以实际代码为准。*
