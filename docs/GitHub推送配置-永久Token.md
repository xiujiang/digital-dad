# GitHub 永久 Token 配置（对本机所有项目生效）

按顺序在终端执行以下命令即可。将 `YOUR_GITHUB_TOKEN` 替换为你的 Personal Access Token。

---

## 一、让 Git 永久记住 GitHub 凭据（对本机所有项目生效）

```bash
# 1. 启用凭据存储（一次配置，对本机所有 Git 项目生效）
git config --global credential.helper store

# 2. 把 GitHub 的账号和 Token 写入凭据文件（替换下面的 YOUR_GITHUB_TOKEN）
#    Linux/macOS：
echo "https://xiujiang:YOUR_GITHUB_TOKEN@github.com" >> ~/.git-credentials
chmod 600 ~/.git-credentials

# Windows 在「命令提示符」或 PowerShell 中：
# (echo https://xiujiang:YOUR_GITHUB_TOKEN@github.com) >> %USERPROFILE%\.git-credentials
```

说明：
- `xiujiang` 是你的 GitHub 用户名，Token 时用用户名即可。
- 执行后，本机所有 `git push` / `git pull` 到 `github.com` 都会自动使用这个 Token，无需每次输入。

---

## 二、推送当前项目（digital-dad）

在项目目录下执行：

```bash
cd /volume1/A1D/projectWorkspace/digital-dad
git push -u origin main
```

之后本仓库的推送只需：

```bash
git push
```

---

## 三、若你使用其他 GitHub 账号

多账号时，凭据按「域名」区分。上面配置的是 `https://github.com`，所以只对 GitHub 生效；其他 Git 服务器需在 `~/.git-credentials` 中另加一行，例如：

```
https://用户名:Token@github.com
https://其他用户名:其他Token@gitlab.com
```

每行一个 `https://用户:Token@主机`，保存后对所有使用该主机的项目生效。
