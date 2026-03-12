#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数字爸爸 - 本地部署到云端服务器

用法: python deploy.py
依赖: pip install paramiko
"""

import os
import shutil
import sys
import tempfile

try:
    import paramiko
except ImportError:
    print("[错误] 请先安装 paramiko: pip install paramiko")
    sys.exit(1)

# ---------- 配置（请修改为你的服务器信息）----------
SERVER = "ubuntu@101.34.64.224"  # 如 root@101.34.64.224
PASSWORD = os.getenv("DEPLOY_PASSWORD", "Shuzibaba@888")  # 优先从环境变量读取
REMOTE_DIR = "/opt/digital-dad"  # 服务器上的项目路径

# 排除的目录和文件（不部署到服务器）
EXCLUDE = {"target", ".git", ".idea", ".vscode", "node_modules", "deploy-temp", "__pycache__"}


def ignore_func(dirname, names):
    """shutil.copytree 的 ignore 函数"""
    return [n for n in names if n in EXCLUDE or n.endswith(".log")]


def sftp_upload_dir(sftp, local_dir, remote_dir):
    """递归上传目录到远程"""
    for name in os.listdir(local_dir):
        local_path = os.path.join(local_dir, name)
        remote_path = f"{remote_dir.rstrip('/')}/{name}"
        if os.path.isdir(local_path):
            try:
                sftp.mkdir(remote_path)
            except OSError:
                pass  # 目录已存在
            sftp_upload_dir(sftp, local_path, remote_path)
        else:
            sftp.put(local_path, remote_path)


def main():
    print("========== 数字爸爸 部署脚本 ==========\n")

    # 检查配置
    if "你的服务器" in SERVER or SERVER == "root@":
        print("[错误] 请先修改 deploy.py 中的 SERVER 配置（服务器 IP 和用户）")
        sys.exit(1)

    user, host = SERVER.split("@", 1) if "@" in SERVER else (None, SERVER)
    if not user:
        print("[错误] SERVER 格式应为 user@host")
        sys.exit(1)

    project_root = os.path.dirname(os.path.abspath(__file__))
    temp_dir = os.path.join(tempfile.gettempdir(), "digital-dad-deploy")

    # 1. 复制项目文件
    print("[1/4] 复制项目文件（排除 target、.git）...")
    if os.path.exists(temp_dir):
        shutil.rmtree(temp_dir)
    shutil.copytree(project_root, temp_dir, ignore=ignore_func)

    # 2. SSH 连接并上传
    remote_temp = "/tmp/digital-dad-deploy"
    print(f"[2/4] 上传到服务器 {SERVER} ...")

    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        ssh.connect(host, username=user, password=PASSWORD, timeout=30)
    except Exception as e:
        print(f"[错误] SSH 连接失败: {e}")
        shutil.rmtree(temp_dir, ignore_errors=True)
        sys.exit(1)

    # 远程创建临时目录
    stdin, stdout, stderr = ssh.exec_command(f"rm -rf {remote_temp}; mkdir -p {remote_temp}")
    stdout.channel.recv_exit_status()

    # SFTP 上传
    sftp = ssh.open_sftp()
    for name in os.listdir(temp_dir):
        local_path = os.path.join(temp_dir, name)
        remote_path = f"{remote_temp}/{name}"
        if os.path.isdir(local_path):
            sftp.mkdir(remote_path)
            sftp_upload_dir(sftp, local_path, remote_path)
        else:
            sftp.put(local_path, remote_path)
    sftp.close()

    # 3. 在服务器上部署
    print("[3/4] 在服务器上部署...")
    # sudo mv 后需 chown 以便当前用户能运行 docker compose
    deploy_cmd = f"sudo rm -rf {REMOTE_DIR}; sudo mv {remote_temp} {REMOTE_DIR}; sudo chown -R {user}:{user} {REMOTE_DIR}; cd {REMOTE_DIR} && docker compose up -d --build"
    stdin, stdout, stderr = ssh.exec_command(deploy_cmd, get_pty=True)
    # 读取输出（忽略编码/ANSI 字符导致的打印异常）
    for line in stdout:
        try:
            print(line.rstrip())
        except (UnicodeEncodeError, UnicodeDecodeError):
            pass
    exit_code = stdout.channel.recv_exit_status()
    ssh.close()

    # 4. 清理
    shutil.rmtree(temp_dir, ignore_errors=True)

    if exit_code != 0:
        print(f"[错误] Docker 启动失败，退出码: {exit_code}")
        sys.exit(1)

    server_host = host
    print("\n[4/4] 部署完成！")
    print(f"应用地址: http://{server_host}:8080")
    print(f"查看日志: ssh {SERVER} 'cd {REMOTE_DIR} && docker compose logs -f app'\n")


if __name__ == "__main__":
    main()
