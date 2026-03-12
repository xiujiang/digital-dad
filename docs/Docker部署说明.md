# 数字爸爸 - Docker 部署说明

## 一、前置要求

- Docker 20.10+
- Docker Compose 2.0+
- 已有 MySQL 服务（表结构需按 scripts/sql/ 顺序初始化）

## 二、配置说明

数据库、JWT 等配置均在 `src/main/resources/application.yml` 中，构建时会打包进镜像。

部署前请确认 `application.yml` 中已正确配置：

- `spring.datasource.*`：数据库连接
- `app.base-url`：C 端分享链接 base（生产环境改为实际域名）
- `jwt.secret`：JWT 密钥（生产环境务必修改）

## 三、部署方式

```bash
# 构建并启动
docker-compose up -d

# 查看日志
docker-compose logs -f app
```

应用端口：8080

## 四、可选：环境变量覆盖

若需在运行期覆盖配置（不修改代码），可在 docker-compose.yml 的 `environment` 中取消注释并设置，例如：

```yaml
environment:
  APP_BASE_URL: https://your-domain.com
  SPRING_DATASOURCE_URL: jdbc:mysql://...
```

## 五、常用命令

```bash
# 停止
docker-compose down

# 重新构建并启动
docker-compose up -d --build
```

## 六、生产环境注意

1. **JWT_SECRET**：务必改为强随机字符串（≥32 字符）
2. **APP_BASE_URL**：改为实际对外域名，供分享链接、二维码使用
3. **HTTPS**：应用端口 8080，若需 HTTPS 建议在 Nginx 等反向代理层配置
