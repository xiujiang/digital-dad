# 数字爸爸 v0.1 MVP 接口文档

> 基于需求总览整理的完整接口文档，涵盖入参、出参及调用说明。

---

## 一、通用说明

### 1.1 响应结构

所有接口统一返回 `Result<T>` 结构：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```


| 字段      | 类型     | 说明           |
| ------- | ------ | ------------ |
| code    | int    | 状态码，200 表示成功 |
| message | string | 提示信息         |
| data    | object | array        |


### 1.2 认证方式

- **B 端（主持人）**：需登录，请求头携带 `Authorization: Bearer {token}`
- **C 端（参与者）**：部分接口需登录；扫码入口 `/api/c/entry/{token}` 可匿名访问
- **登录获取 token**：调用 `POST /api/auth/login`，响应中 `token` 置于请求头

### 1.3 错误码


| code | 说明            |
| ---- | ------------- |
| 200  | 成功            |
| 400  | 参数错误 / 业务校验失败 |
| 403  | 无权限           |
| 404  | 资源不存在         |
| 500  | 服务器异常         |


---

## 二、认证接口（登录 / 主持人 / 参与者共用）

### 2.1 发送验证码

**POST** `/api/auth/send-code`

**认证**：无需

**请求体**：

```json
{
  "phone": "13800138000"
}
```


| 字段    | 类型     | 必填  | 说明           |
| ----- | ------ | --- | ------------ |
| phone | string | 是   | 11 位手机号，1 开头 |


**响应**：`Result<Void>`，data 为 null

---

### 2.2 登录

**POST** `/api/auth/login`

**认证**：无需

**请求体**：

```json
{
  "phone": "13800138000",
  "code": "123456"
}
```


| 字段    | 类型     | 必填  | 说明       |
| ----- | ------ | --- | -------- |
| phone | string | 是   | 11 位手机号  |
| code  | string | 是   | 6 位数字验证码 |


**响应**：`Result<LoginResponse>`

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "userType": "HOST",
    "name": "张三",
    "phone": "13800138000"
  }
}
```


| 字段       | 类型     | 说明              |
| -------- | ------ | --------------- |
| token    | string | JWT，用于后续请求头     |
| userId   | long   | 用户 ID           |
| userType | string | 用户类型：HOST（主持人）等 |
| name     | string | 姓名              |
| phone    | string | 手机号             |


---

### 2.3 退出登录

**POST** `/api/auth/logout`

**认证**：需登录

**请求体**：无

**响应**：`Result<Void>`

---

### 2.4 获取当前用户

**GET** `/api/auth/me`

**认证**：需登录

**响应**：`Result<CurrentUserResponse>`

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1,
    "userType": "HOST",
    "name": "张三",
    "phone": "13800138000",
    "avatarUrl": null
  }
}
```

---

## 三、C 端接口（参与者：新郎 / 新娘）

### 3.1 扫码入口 - 获取项目信息

**GET** `/api/c/entry/{token}`

**认证**：无需（匿名，用于 H5 扫码进入）

**路径参数**：


| 参数    | 类型     | 说明                      |
| ----- | ------ | ----------------------- |
| token | string | 分享令牌（shareToken），来自分享链接 |


**响应**：`Result<ProjectInfoResponse>`

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "projectId": 1,
    "groomName": "张三",
    "brideName": "李四",
    "weddingDate": "2025-06-01"
  }
}
```

---

### 3.2 选择身份并绑定

**POST** `/api/c/projects/{projectId}/bind`

**认证**：需登录（C 端用户）

**路径参数**：


| 参数        | 类型   | 说明    |
| --------- | ---- | ----- |
| projectId | long | 项目 ID |


**请求体**：

```json
{
  "role": "GROOM"
}
```


| 字段   | 类型     | 必填  | 说明                      |
| ---- | ------ | --- | ----------------------- |
| role | string | 是   | 角色：GROOM（新郎）/ BRIDE（新娘） |


**响应**：`Result<BindParticipantResponse>`

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "projectId": 1,
    "role": "GROOM"
  }
}
```


| 字段            | 类型     | 说明             |
| ------------- | ------ | -------------- |
| projectId     | long   | 项目 ID          |
| role          | string | 已绑定角色          |


---

### 3.3 创建或恢复会话

**POST** `/api/c/sessions`

**认证**：需登录

**请求体**：

```json
{
  "projectId": 3
}
```


| 字段       | 类型   | 必填  | 说明                                   |
| ---------- | ------ | ----- | -------------------------------------- |
| projectId  | long   | 是    | 项目 ID，后端根据当前用户+项目确定参与者 |


**响应**：`Result<SessionResponse>`

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "projectId": 1,
    "currentProjectBoardId": 1,
    "boardCode": "FAMILY_ORIGIN",
    "boardName": "原生家庭",
    "status": "ACTIVE",
    "roundCount": 0,
    "startedAt": "2025-03-01T10:00:00",
    "lastActiveAt": "2025-03-01T10:00:00",
    "createdAt": "2025-03-01T10:00:00",
    "currentBoardOrder": 1,
    "boards": [
      {
        "projectBoardId": 1,
        "boardCode": "FAMILY_ORIGIN",
        "boardName": "原生家庭",
        "displayOrder": 1,
        "isCurrent": true,
        "isCompleted": false
      }
    ]
  }
}
```


| 字段                    | 类型      | 说明                                                              |
| --------------------- | ------- | --------------------------------------------------------------- |
| id                    | long    | 会话 ID                                                           |
| projectId             | long    | 项目 ID                                                           |
| currentProjectBoardId | long    | 当前板块 ID                                                         |
| boardCode             | string  | 当前板块编码                                                          |
| boardName             | string  | 当前板块名称                                                          |
| status                | string  | 会话状态：READY / ACTIVE / WAITING_CONFIRM / COMPLETED / INTERRUPTED |
| roundCount            | int     | 当前轮次                                                            |
| boards                | array   | 项目板块列表（含进度）                                                     |
| boards[].isCurrent    | boolean | 是否为当前板块                                                         |
| boards[].isCompleted  | boolean | 是否已完成                                                           |


---

### 3.4 获取会话详情

**GET** `/api/c/sessions/{sessionId}`

**认证**：需登录

**路径参数**：`sessionId`（long）

**响应**：`Result<SessionResponse>`（结构同 3.3）

---

### 3.5 获取消息列表

**GET** `/api/c/sessions/{sessionId}/messages`

**认证**：需登录

**路径参数**：`sessionId`（long）

**响应**：`Result<List<MessageResponse>>`

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "sessionId": 1,
      "senderType": "USER",
      "messageType": "TEXT",
      "content": "我的童年...",
      "audioUrl": null,
      "sequenceNo": 1,
      "batchNo": 1,
      "isSubmitted": true,
      "createdAt": "2025-03-01T10:01:00"
    }
  ]
}
```


| 字段          | 类型      | 说明                 |
| ----------- | ------- | ------------------ |
| senderType  | string  | USER / AI / SYSTEM |
| messageType | string  | TEXT / AUDIO       |
| isSubmitted | boolean | 是否已提交给 AI（未提交可删改）  |


---

### 3.6 发送消息

**POST** `/api/c/sessions/{sessionId}/messages`

**认证**：需登录

**路径参数**：`sessionId`（long）

**请求体**：

```json
{
  "content": "我的童年是在农村度过的",
  "audioUrl": null,
  "transcriptText": null
}
```


| 字段             | 类型     | 必填  | 说明                    |
| -------------- | ------ | --- | --------------------- |
| content        | string | 否   | 文本内容，最长 5000 字        |
| audioUrl       | string | 否   | 语音 URL                |
| transcriptText | string | 否   | 语音转写文本（无 content 时可传） |


**响应**：`Result<MessageResponse>`（结构同 3.5 单条）

---

### 3.7 修改未提交消息

**PUT** `/api/c/sessions/{sessionId}/messages/{messageId}`

**认证**：需登录

**路径参数**：`sessionId`、`messageId`（long）

**请求体**：

```json
{
  "content": "修改后的内容"
}
```


| 字段      | 类型     | 必填  | 说明             |
| ------- | ------ | --- | -------------- |
| content | string | 否   | 文本内容，最长 5000 字 |


**响应**：`Result<MessageResponse>`

---

### 3.8 删除未提交消息

**DELETE** `/api/c/sessions/{sessionId}/messages/{messageId}`

**认证**：需登录

**路径参数**：`sessionId`、`messageId`（long）

**响应**：`Result<Void>`

---

### 3.9 提交消息给 AI

**POST** `/api/c/sessions/{sessionId}/submit`

**认证**：需登录

**路径参数**：`sessionId`（long）

**说明**：将当前未提交消息批量提交，触发 AI 回复

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
        "content": "感谢你的分享...",
        "sequenceNo": 2,
        "batchNo": 1,
        "isSubmitted": true,
        "createdAt": "2025-03-01T10:02:00"
      }
    ]
  }
}
```

---

### 3.10 创建板块小结

**POST** `/api/c/sessions/{sessionId}/summaries`

**认证**：需登录

**路径参数**：`sessionId`（long）

**说明**：根据当前板块对话生成小结（板块结束时调用）

**请求体**：无

**响应**：`Result<BoardSummaryResponse>`

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "sessionId": 1,
    "projectBoardId": 1,
    "boardCode": "FAMILY_ORIGIN",
    "boardName": "原生家庭",
    "versionNo": 1,
    "status": "WAITING_CONFIRM",
    "title": "原生家庭小结",
    "generatedAt": "2025-03-01T10:10:00",
    "confirmedAt": null,
    "items": [
      {
        "id": 1,
        "summaryId": 1,
        "itemType": "FACT",
        "content": "童年在小镇度过",
        "itemOrder": 0,
        "isSelected": true,
        "createdAt": "2025-03-01T10:10:00"
      }
    ]
  }
}
```


| 字段         | 说明                                              |
| ---------- | ----------------------------------------------- |
| status     | DRAFT / GENERATED / WAITING_CONFIRM / CONFIRMED |
| itemType   | FACT（事实类）/ EXPRESSION（表达类）                      |
| isSelected | 是否勾选纳入素材                                        |


---

### 3.11 获取当前板块小结

**GET** `/api/c/sessions/{sessionId}/summaries/current`

**认证**：需登录

**路径参数**：`sessionId`（long）

**响应**：`Result<BoardSummaryResponse>` 或 `data` 为 null（无小结时）

---

### 3.12 获取小结详情

**GET** `/api/c/board-summaries/{summaryId}`

**认证**：需登录

**路径参数**：`summaryId`（long）

**响应**：`Result<BoardSummaryResponse>`

---

### 3.13 修改小结条目

**PUT** `/api/c/summary-items/{itemId}`

**认证**：需登录

**路径参数**：`itemId`（long）

**请求体**：

```json
{
  "content": "修改后的要点",
  "itemType": "FACT",
  "isSelected": true
}
```


| 字段         | 类型      | 必填  | 说明                |
| ---------- | ------- | --- | ----------------- |
| content    | string  | 否   | 内容，最长 500 字       |
| itemType   | string  | 否   | FACT / EXPRESSION |
| isSelected | boolean | 否   | 是否勾选              |


**响应**：`Result<SummaryItemResponse>`

---

### 3.14 新增小结条目

**POST** `/api/c/board-summaries/{summaryId}/items`

**认证**：需登录

**路径参数**：`summaryId`（long）

**请求体**：

```json
{
  "content": "补充的要点",
  "itemType": "FACT"
}
```


| 字段       | 类型     | 必填  | 说明          |
| -------- | ------ | --- | ----------- |
| content  | string | 是   | 内容，最长 500 字 |
| itemType | string | 否   | 默认 FACT     |


**响应**：`Result<SummaryItemResponse>`

---

### 3.15 删除小结条目

**DELETE** `/api/c/summary-items/{itemId}`

**认证**：需登录

**路径参数**：`itemId`（long）

**响应**：`Result<Void>`

---

### 3.16 确认小结

**POST** `/api/c/board-summaries/{summaryId}/confirm`

**认证**：需登录

**路径参数**：`summaryId`（long）

**说明**：确认后形成素材快照，该板块素材方可被交付物生成使用

**响应**：`Result<Void>`

---

### 3.17 创建故事/时光

**POST** `/api/c/sessions/{sessionId}/stories`

**认证**：需登录

**路径参数**：`sessionId`（long）

**说明**：根据当前板块对话生成故事叙述

**响应**：`Result<BoardStoryResponse>`

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "sessionId": 1,
    "projectBoardId": 1,
    "boardCode": "FAMILY_ORIGIN",
    "boardName": "原生家庭",
    "content": "这是一段关于原生家庭的温暖记忆...",
    "versionNo": 1,
    "createdAt": "2025-03-01T10:15:00"
  }
}
```

---

### 3.18 获取故事

**GET** `/api/c/sessions/{sessionId}/stories?projectBoardId={projectBoardId}`

**认证**：需登录

**路径参数**：`sessionId`（long）

**查询参数**：


| 参数             | 类型   | 必填  | 说明    |
| -------------- | ---- | --- | ----- |
| projectBoardId | long | 是   | 板块 ID |


**响应**：`Result<BoardStoryResponse>` 或 `data` 为 null（无故事时）

---

### 3.19 获取关键人物列表

**GET** `/api/c/sessions/{sessionId}/persons`

**认证**：需登录

**路径参数**：`sessionId`（long）

**响应**：`Result<List<KeyPersonResponse>>`

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "sessionId": 1,
      "name": "爷爷",
      "roleLabel": "长辈",
      "createdAt": "2025-03-01T10:00:00"
    }
  ]
}
```

---

### 3.20 新增关键人物

**POST** `/api/c/sessions/{sessionId}/persons`

**认证**：需登录

**路径参数**：`sessionId`（long）

**请求体**：

```json
{
  "name": "爷爷",
  "roleLabel": "长辈"
}
```


| 字段        | 类型     | 必填  | 说明           |
| --------- | ------ | --- | ------------ |
| name      | string | 是   | 称谓，最长 50 字   |
| roleLabel | string | 否   | 角色标签，最长 50 字 |


**响应**：`Result<KeyPersonResponse>`

---

### 3.21 修改关键人物

**PUT** `/api/c/key-persons/{personId}`

**认证**：需登录

**路径参数**：`personId`（long）

**请求体**：

```json
{
  "name": "外公",
  "roleLabel": "长辈"
}
```


| 字段        | 类型     | 必填  | 说明   |
| --------- | ------ | --- | ---- |
| name      | string | 否   | 称谓   |
| roleLabel | string | 否   | 角色标签 |


**响应**：`Result<KeyPersonResponse>`

---

### 3.22 删除关键人物

**DELETE** `/api/c/key-persons/{personId}`

**认证**：需登录

**路径参数**：`personId`（long）

**响应**：`Result<Void>`

---

## 四、B 端接口（主持人）

### 4.1 创建项目

**POST** `/api/b/projects`

**认证**：需登录（主持人）

**请求体**：

```json
{
  "groomName": "张三",
  "brideName": "李四",
  "weddingDate": "2025-06-01"
}
```


| 字段          | 类型     | 必填  | 说明              |
| ----------- | ------ | --- | --------------- |
| groomName   | string | 是   | 新郎姓名，2-50 字     |
| brideName   | string | 是   | 新娘姓名，2-50 字     |
| weddingDate | string | 否   | 婚礼日期，yyyy-MM-dd |


**响应**：`Result<ProjectDetailResponse>`

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "projectNo": "P202503010001",
    "groomName": "张三",
    "brideName": "李四",
    "weddingDate": "2025-06-01",
    "status": "ACTIVE",
    "shareToken": "xxx-xxx-xxx",
    "createdAt": "2025-03-01T09:00:00",
    "participants": [
      {
        "id": 1,
        "roleType": "GROOM",
        "roleName": "新郎",
        "status": "INVITED",
        "currentBoardOrder": null,
        "joinedAt": null,
        "lastActiveAt": null,
        "bound": false
      }
    ],
    "contents": []
  }
}
```

---

### 4.2 项目列表

**GET** `/api/b/projects?page=1&size=10`

**认证**：需登录

**查询参数**：


| 参数   | 类型  | 默认  | 说明   |
| ---- | --- | --- | ---- |
| page | int | 1   | 页码   |
| size | int | 10  | 每页条数 |


**响应**：`Result<Page<ProjectListItemResponse>>`

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "projectNo": "P202503010001",
        "groomName": "张三",
        "brideName": "李四",
        "weddingDate": "2025-06-01",
        "status": "ACTIVE",
        "createdAt": "2025-03-01T09:00:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 10,
    "number": 0
  }
}
```

---

### 4.3 项目详情

**GET** `/api/b/projects/{id}`

**认证**：需登录

**路径参数**：`id`（long，项目 ID）

**响应**：`Result<ProjectDetailResponse>`（结构同 4.1，含 participants、contents）

---

### 4.4 分享入口

**GET** `/api/b/projects/{id}/share`

**认证**：需登录

**路径参数**：`id`（long，项目 ID）

**响应**：`Result<ShareEntryResponse>`

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "shareUrl": "https://xxx.com/c/entry/xxx-xxx-xxx",
    "shareToken": "xxx-xxx-xxx",
    "projectId": 1
  }
}
```


| 字段         | 说明             |
| ---------- | -------------- |
| shareUrl   | 分享入口链接（发给新人扫码） |
| shareToken | 令牌，可用于拼接二维码等   |
| projectId  | 项目 ID          |


---

### 4.5 生成交付物

**POST** `/api/b/projects/{projectId}/deliverables/generate`

**认证**：需登录

**路径参数**：`projectId`（long）

**请求体**：

```json
{
  "contentType": "OPENING_SPEECH"
}
```


| 字段          | 类型     | 必填  | 说明                                                    |
| ----------- | ------ | --- | ----------------------------------------------------- |
| contentType | string | 是   | OPENING_SPEECH（开场白）/ GROOM_VOW（新郎誓言）/ BRIDE_VOW（新娘誓言） |


**说明**：需对应参与者已有确认素材；开场白需新郎+新娘均有确认素材

**响应**：`Result<DeliverableDetailResponse>`

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "projectId": 1,
    "contentType": "OPENING_SPEECH",
    "contentTypeName": "婚礼开场白",
    "versionNo": 1,
    "title": "婚礼开场白",
    "content": "尊敬的各位来宾，大家好...",
    "status": "ACTIVE",
    "snapshotVersionAt": "2025-03-01T12:00:00",
    "createdAt": "2025-03-01T12:00:00",
    "updatedAt": "2025-03-01T12:00:00"
  }
}
```


| 字段   | 说明                                |
| ------ | ----------------------------------- |
| status | DRAFT / ACTIVE / OUTDATED（素材有更新时） |


---

### 4.6 按类型获取交付物

**GET** `/api/b/projects/{projectId}/deliverables/{contentType}`

**认证**：需登录

**路径参数**：


| 参数          | 说明                                     |
| ----------- | -------------------------------------- |
| projectId   | long                                   |
| contentType | OPENING_SPEECH / GROOM_VOW / BRIDE_VOW |


**响应**：`Result<DeliverableDetailResponse>`

---

### 4.7 按 ID 获取交付物

**GET** `/api/b/deliverables/{id}`

**认证**：需登录

**路径参数**：`id`（long，交付物 ID）

**响应**：`Result<DeliverableDetailResponse>`

---

### 4.8 编辑交付物

**PUT** `/api/b/deliverables/{id}`

**认证**：需登录

**路径参数**：`id`（long）

**请求体**：

```json
{
  "title": "婚礼开场白",
  "content": "修改后的全文内容..."
}
```


| 字段      | 类型     | 必填  | 说明          |
| ------- | ------ | --- | ----------- |
| title   | string | 否   | 标题，最长 100 字 |
| content | string | 否   | 正文          |


**响应**：`Result<DeliverableDetailResponse>`

---

### 4.9 删除交付物

**DELETE** `/api/b/deliverables/{id}`

**认证**：需登录

**路径参数**：`id`（long）

**响应**：`Result<Void>`

---

## 五、B 端扩展接口（项目 / 板块配置）

### 5.1 获取启用板块列表

**GET** `/api/b/board-meta/enabled`

**认证**：需登录

**说明**：获取可配置的板块元数据（新建项目选板块时使用）

**响应**：`Result<List<BoardMetaResponse>>`

---

### 5.2 获取项目板块列表

**GET** `/api/b/projects/{projectId}/boards`

**认证**：需登录

**路径参数**：`projectId`（long）

**响应**：`Result<List<ProjectBoardResponse>>`

---

### 5.3 为项目添加板块

**POST** `/api/b/projects/{projectId}/boards`

**认证**：需登录

**路径参数**：`projectId`（long）

**请求体**：`CreateProjectBoardRequest`（含 boardMetaId、displayOrder 等）

**响应**：`Result<ProjectBoardResponse>`

---

### 5.4 主持人个人信息

**GET** `/api/b/users/me`

**认证**：需登录

**响应**：`Result<CurrentUserResponse>`

---

## 六、接口一览表


| 端   | 分类  | 方法     | 路径                                                     | 说明       |
| --- | --- | ------ | ------------------------------------------------------ | -------- |
| 公共  | 认证  | POST   | /api/auth/send-code                                    | 发送验证码    |
| 公共  | 认证  | POST   | /api/auth/login                                        | 登录       |
| 公共  | 认证  | POST   | /api/auth/logout                                       | 退出       |
| 公共  | 认证  | GET    | /api/auth/me                                           | 当前用户     |
| C   | 入口  | GET    | /api/c/entry/{token}                                   | 扫码获取项目信息 |
| C   | 入口  | POST   | /api/c/projects/{projectId}/bind                       | 选择身份绑定   |
| C   | 会话  | POST   | /api/c/sessions                                        | 创建/恢复会话  |
| C   | 会话  | GET    | /api/c/sessions/{sessionId}                            | 会话详情     |
| C   | 会话  | GET    | /api/c/sessions/{sessionId}/messages                   | 消息列表     |
| C   | 会话  | POST   | /api/c/sessions/{sessionId}/messages                   | 发送消息     |
| C   | 会话  | PUT    | /api/c/sessions/{sessionId}/messages/{messageId}       | 修改消息     |
| C   | 会话  | DELETE | /api/c/sessions/{sessionId}/messages/{messageId}       | 删除消息     |
| C   | 会话  | POST   | /api/c/sessions/{sessionId}/submit                     | 提交给 AI   |
| C   | 小结  | POST   | /api/c/sessions/{sessionId}/summaries                  | 创建小结     |
| C   | 小结  | GET    | /api/c/sessions/{sessionId}/summaries/current          | 当前小结     |
| C   | 小结  | GET    | /api/c/board-summaries/{summaryId}                     | 小结详情     |
| C   | 小结  | PUT    | /api/c/summary-items/{itemId}                          | 修改条目     |
| C   | 小结  | POST   | /api/c/board-summaries/{summaryId}/items               | 新增条目     |
| C   | 小结  | DELETE | /api/c/summary-items/{itemId}                          | 删除条目     |
| C   | 小结  | POST   | /api/c/board-summaries/{summaryId}/confirm             | 确认小结     |
| C   | 故事  | POST   | /api/c/sessions/{sessionId}/stories                    | 创建故事     |
| C   | 故事  | GET    | /api/c/sessions/{sessionId}/stories                    | 获取故事     |
| C   | 人物  | GET    | /api/c/sessions/{sessionId}/persons                    | 人物列表     |
| C   | 人物  | POST   | /api/c/sessions/{sessionId}/persons                    | 新增人物     |
| C   | 人物  | PUT    | /api/c/key-persons/{personId}                          | 修改人物     |
| C   | 人物  | DELETE | /api/c/key-persons/{personId}                          | 删除人物     |
| B   | 项目  | POST   | /api/b/projects                                        | 创建项目     |
| B   | 项目  | GET    | /api/b/projects                                        | 项目列表     |
| B   | 项目  | GET    | /api/b/projects/{id}                                   | 项目详情     |
| B   | 项目  | GET    | /api/b/projects/{id}/share                             | 分享入口     |
| B   | 交付物 | POST   | /api/b/projects/{projectId}/deliverables/generate      | 生成交付物    |
| B   | 交付物 | GET    | /api/b/projects/{projectId}/deliverables/{contentType} | 按类型获取    |
| B   | 交付物 | GET    | /api/b/deliverables/{id}                               | 按 ID 获取  |
| B   | 交付物 | PUT    | /api/b/deliverables/{id}                               | 编辑       |
| B   | 交付物 | DELETE | /api/b/deliverables/{id}                               | 删除       |
| B   | 扩展  | GET    | /api/b/board-meta/enabled                              | 启用板块     |
| B   | 扩展  | GET    | /api/b/projects/{projectId}/boards                     | 项目板块     |
| B   | 扩展  | POST   | /api/b/projects/{projectId}/boards                     | 添加板块     |
| B   | 扩展  | GET    | /api/b/users/me                                        | 主持人信息    |


---

*文档版本：v0.1 MVP，与需求总览一致。*