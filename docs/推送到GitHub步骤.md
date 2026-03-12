# 将项目推送到 GitHub 的步骤

## 一、在 GitHub 上创建仓库（你需要在网页完成）

1. 登录 [GitHub](https://github.com)。
2. 右上角 **"+"** → **"New repository"**。
3. 填写：
   - **Repository name**：例如 `digital-dad`（或你想要的英文名）。
   - **Description**（可选）：项目简介。
   - 选择 **Public** 或 **Private**。
   - **不要**勾选 "Add a README file"（本地已有代码，避免冲突）。
4. 点击 **"Create repository"**。
5. 创建完成后，在仓库页复制 **HTTPS 地址**，形如：  
   `https://github.com/你的用户名/digital-dad.git`  
   或 **SSH 地址**：  
   `git@github.com:你的用户名/digital-dad.git`

---

## 二、在本地配置 Git 用户（仅首次需要）

若从未配置过，在终端执行（把名字和邮箱换成你的）：

```bash
git config --global user.name "你的名字或昵称"
git config --global user.email "你的GitHub邮箱"
```

---

## 三、在项目目录执行以下命令

在项目根目录 `/volume1/A1D/projectWorkspace/digital-dad` 下执行：

```bash
# 1. 添加所有文件（.gitignore 已排除 target、.idea 等）
git add .

# 2. 首次提交
git commit -m "Initial commit: 数字爸爸项目"

# 3. 添加远程仓库（把下面的 URL 换成你在 GitHub 复制的地址）
git remote add origin https://github.com/你的用户名/digital-dad.git

# 4. 推送到 GitHub（分支名为 main）
git push -u origin main
```

若 GitHub 仓库用的是 **SSH**，第 3 步改为：

```bash
git remote add origin git@github.com:你的用户名/digital-dad.git
```

---

## 四、若推送时提示需要登录

- **HTTPS**：会提示输入 GitHub 用户名和密码；密码处需使用 **Personal Access Token**（在 GitHub → Settings → Developer settings → Personal access tokens 创建）。
- **SSH**：需先在本地配置 SSH 公钥并添加到 GitHub（Settings → SSH and GPG keys），再用上面的 SSH 地址推送。

---

## 五、当前本地已完成的准备

- 已在项目目录执行 `git init`，并已将默认分支设为 `main`。
- `.gitignore` 已包含：`target/`、`.idea/`、`*.iml`、`application-local.yml`、`.env` 等，避免把构建产物和敏感配置推上去。

你只需：**在 GitHub 建好仓库 → 复制仓库 URL → 在项目目录执行上面的 `git add`、`git commit`、`git remote add`、`git push`** 即可。
