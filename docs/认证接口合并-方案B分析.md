# 认证接口合并 - 方案 B 改动分析

**目标**：只保留一套认证接口（`/api/auth/*`），通过**请求参数**区分「主持人登录」与「超管登录」；登录后获取个人信息等统一用同一套接口，角色由 Token 决定，无需再传参。

---

## 一、涉及的能力与接口

| 能力           | 当前实现                           | 方案 B 后                          |
|----------------|------------------------------------|------------------------------------|
| 发送验证码     | `POST /api/auth/send-code`         | 不变，仍共用（不区分角色）         |
| 登录           | 主持人：`POST /api/auth/login`<br>超管：`POST /api/admin/auth/login` | **统一** `POST /api/auth/login`，请求体增加可选 `admin` |
| 获取当前用户   | `GET /api/auth/me`<br>`GET /api/admin/auth/me`（与上重复） | **只保留** `GET /api/auth/me`，角色由 Token 解析 |
| 退出登录       | `POST /api/auth/logout`            | 不变                               |

**结论**：需要改动的只有「登录」的入口与参数；`/me` 与 `logout` 只是删掉 admin 那套路径，逻辑已是共用。

---

## 二、请求 / 响应契约变化

### 2.1 登录请求 `LoginRequest`

**当前**：

```json
{
  "phone": "13800138000",
  "code": "123456"
}
```

**方案 B**：增加可选字段，用于区分角色。

```json
{
  "phone": "13800138000",
  "code": "123456",
  "admin": false
}
```

| 字段   | 类型    | 必填 | 说明 |
|--------|---------|------|------|
| phone  | string  | 是   | 手机号，格式不变 |
| code   | string  | 是   | 验证码，6 位数字 |
| admin  | boolean | 否   | 默认 `false`。`true` 表示按「超管」方式登录（必须具有 SUPER_ADMIN 角色） |

**后端约定**：

- 不传 `admin` 或 `admin == false` → 走现有 `AuthService.login()`（主持人登录：无则自动注册 + HOST）。
- `admin == true` → 走现有 `AuthService.adminLogin()`（超管登录：必须有 SUPER_ADMIN，否则 401）。

### 2.2 登录响应 `LoginResponse`

无变更。已包含 `token`、`userId`、`userType`、`roles`、`name`、`phone`，前端可根据 `userType` 或 `roles` 区分主持人/超管。

### 2.3 获取当前用户 `GET /api/auth/me`

- 无需新增参数：当前用户与角色由 JWT 中的 `userId` + `roles` 决定。
- 响应 `CurrentUserResponse` 已含 `userType`、`roles`，无需改。

---

## 三、后端改动点

### 3.1 DTO：`LoginRequest`

- 文件：`com.digitaldad.user.dto.LoginRequest`
- 改动：增加可选字段 `private Boolean admin;`（或 `boolean admin` 默认 false，用包装类型更易区分「未传」与 `false`，建议 `Boolean`，默认按 `false` 处理）。

### 3.2 Controller：`AuthController`

- 文件：`com.digitaldad.user.controller.AuthController`
- 改动：
  - **登录**：`login` 方法从 `LoginRequest` 中读取 `admin`，若为 `Boolean.TRUE` 则调用 `authService.adminLogin(phone, code)`，否则调用 `authService.login(phone, code)`。
  - 其它方法（send-code、logout、me）不变。

### 3.3 删除 `AdminAuthController`

- 文件：`com.digitaldad.user.controller.AdminAuthController`
- 改动：整个类删除；其提供的 `POST /api/admin/auth/login` 与 `GET /api/admin/auth/me` 由 `/api/auth` 统一提供。

### 3.4 Security 配置：`SecurityConfig`

- 文件：`com.digitaldad.common.config.SecurityConfig`
- 改动：
  - **白名单**：删除对 `"/api/admin/auth/login"` 的 `permitAll()`；登录统一走 `POST /api/auth/login`，已为 permitAll 则无需新增。
  - **admin 路径**：若没有其它 `/api/admin/auth/*` 需求，可不再单独配置 `/api/admin/auth/**`；保留 `requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")` 即可（其它 admin 接口仍受保护）。

### 3.5 Service：`AuthService`

- 无需改。继续提供 `login()` 与 `adminLogin()`，由 Controller 根据参数二选一调用。

---

## 四、行为与边界情况

| 场景 | 行为 |
|------|------|
| 主持人登录 | `admin` 不传或 `false` → `login()`，未注册则自动注册并赋 HOST，返回 token + roles。 |
| 超管登录 | `admin == true` → `adminLogin()`，无 SUPER_ADMIN 则 401「该手机号未开通超管账号」。 |
| 同一手机号既有 HOST 又有 SUPER_ADMIN | 同一用户；传 `admin: false` 得到主持人身份，`admin: true` 得到超管身份，token 内 roles 不同用途由后端根据本次登录方式决定（当前实现是同一用户同一批 roles，仅登录时校验不同：admin 时要求含 SUPER_ADMIN）。 |
| 只传 `admin: true` 但该用户无 SUPER_ADMIN | `adminLogin()` 内校验不通过，返回 401，文案已存在。 |
| 获取个人信息 | 主持人/超管都只调 `GET /api/auth/me`，后端根据 Token 的 userId 查库并带出 roles，返回同一结构；前端根据 `userType`/`roles` 展示或跳转。 |

**无需新增**：发送验证码不区分角色，逻辑不变；若未来要对「超管登录前先发码」做限流或提示，可再在 send-code 上扩展，本次可不做。

---

## 五、前端改动与兼容

### 5.1 必须改动

- **超管登录**：由原来的 `POST /api/admin/auth/login` 改为 `POST /api/auth/login`，并在 body 中增加 `"admin": true`。
- **超管「当前用户」**：由原来的 `GET /api/admin/auth/me` 改为 `GET /api/auth/me`（与主持人共用）。
- **主持人登录**：可继续只传 `phone`、`code`（不传 `admin` 或传 `admin: false`），保证兼容。

### 5.2 兼容与废弃

- 若希望过渡期内保留旧超管登录地址，可在删除 `AdminAuthController` 前，短期保留一个薄 Controller：`POST /api/admin/auth/login` 收到请求后转发到同一 `AuthService.login(phone, code)` 并**固定传 admin=true 语义**（例如在转发前将 request body 加上 `admin: true` 再调 AuthController 的逻辑，或直接调 `authService.adminLogin()`），并响应 `X-Deprecated: true` 或文档注明废弃，待前端全部切到 `/api/auth/login` 后再删。
- 本次分析建议**直接删除** AdminAuthController，前端一并改为方案 B，避免长期维护两套路径。

---

## 六、文档与回归

### 6.1 需更新的文档

- `docs/API接口文档.md`：删除「超管登录」「超管 /me」独立小节；在「登录」中说明请求体增加 `admin`，以及 `GET /api/auth/me` 为主持人/超管共用。
- `docs/合并主持人与管理员接口-路线分析.md`：在「认证合并」一条中注明已按方案 B 实施（参数区分角色）。
- 若存在 Postman/前端 API 契约：更新登录与 /me 的 base URL 与 body 示例。

### 6.2 回归验证

- 主持人：发送验证码 → `POST /api/auth/login`（不传或 `admin: false`）→ `GET /api/auth/me` → 访问 `/api/b/**`。
- 超管：发送验证码 → `POST /api/auth/login`（`admin: true`）→ `GET /api/auth/me` → 访问 `/api/admin/**`。
- 超管账号错误使用：`admin: true` 但该手机号无 SUPER_ADMIN → 401。
- 旧地址 `POST /api/admin/auth/login`、`GET /api/admin/auth/me` 返回 404（或按你选的兼容方式验证转发与废弃头）。

---

## 七、小结：方案 B 改动清单

| 序号 | 项 | 说明 |
|------|----|------|
| 1 | LoginRequest | 增加可选 `Boolean admin`，默认 false |
| 2 | AuthController.login | 根据 `admin == true` 调用 adminLogin，否则 login |
| 3 | 删除 AdminAuthController | 移除类及映射 |
| 4 | SecurityConfig | 去掉 `/api/admin/auth/login` 的 permitAll，仅保留 /api/auth 白名单 |
| 5 | 文档 | 更新 API 文档与合并路线说明 |
| 6 | 前端 | 超管改用 POST /api/auth/login + admin:true，/me 统一用 GET /api/auth/me |

按上述顺序实施即可完成「一套接口、参数区分角色」的认证合并；后续若再合并项目、板块、用户等接口，可继续沿用「同一路径 + 角色鉴权」的思路。
