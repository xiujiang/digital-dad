# 数字爸爸 - 本地到云端部署 Step by Step

> 从本机将项目部署到云端服务器的完整步骤

---

## 方式一：一键脚本（推荐）

1. 安装依赖：`pip install paramiko`
2. 编辑 `deploy.py`，确认 `SERVER` 和 `PASSWORD` 已正确配置
3. 在项目根目录执行：

```bash
python deploy.py
```

脚本会自动：复制项目（排除 target、.git）→ 上传到服务器 → 启动 Docker 部署。  
> 安全提示：若将代码提交到公开仓库，请勿包含密码，可改为从环境变量 `DEPLOY_PASSWORD` 读取。

---

## 方式二：手动步骤

### 第一步：本地准备

### 1.1 确认 application.yml 配置正确

数据库等配置在 `src/main/resources/application.yml` 中，构建时会打包进镜像。

部署前请确认：

- `spring.datasource.url`：MySQL 地址（如 101.34.64.224:3306）
- `spring.datasource.username` / `password`
- `app.base-url`：生产环境改为实际域名，如 `https://your-domain.com`

### 1.2 确认代码可运行

```powershell
.\mvnw.cmd spring-boot:run
```

---

### 第二步：代码上传到服务器

### 方式 A：使用 Git（推荐）

**2A.1** 本地提交并推送代码到远程仓库（GitHub / Gitee / 自建 Git）

```powershell
git add .
git commit -m "准备部署"
git push origin main
```

**2A.2** 登录云服务器，拉取代码

```bash
# SSH 连接（替换为你的服务器 IP 和用户名）
ssh root@你的服务器IP

# 安装 git（若未安装）
# yum install -y git   # CentOS
# apt install -y git  # Ubuntu

# 克隆项目（替换为你的仓库地址）
cd /opt
git clone https://github.com/你的用户名/digital-dad.git
cd digital-dad
```

### 方式 B：使用 SCP 上传压缩包

**2B.1** 本地打包（排除 target、.git 等）

```powershell
# 在项目根目录，PowerShell
Compress-Archive -Path * -DestinationPath digital-dad.zip -Force
# 注意：会包含 node_modules 等大目录，可先手动删除不需要的
```

**2B.2** 上传到服务器

```powershell
scp digital-dad.zip root@你的服务器IP:/opt/
```

**2B.3** 登录服务器解压

```bash
ssh root@你的服务器IP
cd /opt
unzip digital-dad.zip -d digital-dad
cd digital-dad
```

---

### 第三步：服务器安装 Docker（若未安装）

登录服务器后执行：

```bash
# CentOS / RHEL
yum install -y yum-utils
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
yum install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
systemctl start docker
systemctl enable docker

# Ubuntu / Debian
apt update
apt install -y ca-certificates curl gnupg
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture)] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
apt update
apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
```

验证：

```bash
docker --version
docker compose version
```

---

### 第四步：构建并启动容器

在项目目录执行：

```bash
cd /opt/digital-dad

# 构建镜像并后台启动
docker compose up -d --build
```

首次构建可能需要几分钟。

---

### 第五步：检查是否正常运行

```bash
# 查看容器状态
docker compose ps

# 查看日志（确认无报错）
docker compose logs -f app
```

按 `Ctrl+C` 退出日志。

在浏览器访问：`http://你的服务器IP:8080`

若有健康检查接口：`http://你的服务器IP:8080/api/health`

---

### 第六步：开放 8080 端口（若无法访问）

### 云厂商安全组

- **阿里云**：控制台 → 安全组 → 入方向 → 添加规则：端口 8080，源 0.0.0.0/0
- **腾讯云**：控制台 → 安全组 → 入站规则 → 添加：TCP 8080
- **华为云**：同上，入方向规则放行 8080

### 服务器防火墙

```bash
# CentOS (firewalld)
firewall-cmd --permanent --add-port=8080/tcp
firewall-cmd --reload

# Ubuntu (ufw)
ufw allow 8080
ufw reload
```

---

### 第七步：后续更新部署

代码有更新时：

```bash
cd /opt/digital-dad

# Git 方式：拉取最新代码
git pull

# 重新构建并启动
docker compose up -d --build
```

---

## 常见问题

### 1. 容器启动后马上退出

```bash
docker compose logs app
```

根据日志排查，常见原因：数据库连接失败。

### 2. 数据库连接失败

- 确认 MySQL 对应用服务器放行了 3306 端口
- 确认 `application.yml` 中数据库地址、用户名、密码正确

### 3. 外部无法访问 8080

- 检查云厂商安全组是否放行 8080
- 检查服务器防火墙是否放行 8080
- `curl http://localhost:8080` 在服务器上先测试本机是否可达

### 4. 停止 / 重启

```bash
docker compose down      # 停止并删除容器
docker compose up -d     # 重新启动
```

---

## 简要流程回顾

| 步骤 | 操作 |
|------|------|
| 1 | 本地准备：确认 application.yml 配置、代码可运行 |
| 2 | 代码上传：Git 克隆 或 SCP 上传 |
| 3 | 服务器安装 Docker（若未安装） |
| 4 | `docker compose up -d --build` |
| 5 | 检查日志和浏览器访问 |
| 6 | 安全组 / 防火墙放行 8080 |
| 7 | 后续更新：git pull + docker compose up -d --build |
