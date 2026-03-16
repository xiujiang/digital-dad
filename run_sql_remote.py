#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
在云端服务器上执行 SQL 文件，仅执行 DML（INSERT / UPDATE / DELETE / REPLACE）。

用法:
  python run_sql_remote.py [options] <sql_file.sql> [sql_file2.sql ...]
  python run_sql_remote.py [options] -   # 从 stdin 读取 SQL

选项:
  --dry-run    只解析并列出将要执行的语句，不连接服务器
  --all        执行所有语句（包括 DDL），默认仅 DML
  --direct     本机直连数据库执行（需 MYSQL_HOST 为云端 IP）

环境变量（可选）:
  MYSQL_DOCKER_CONTAINER  服务器上 MySQL 的 Docker 容器名或 ID，设置后通过 docker exec 执行

配置与 deploy.py 一致。默认 SSH 到云端后在服务器上执行 mysql 客户端。
若 MySQL 在服务器上的 Docker 中部署，设置 MYSQL_DOCKER_CONTAINER=容器名 即可通过
  docker exec -i <容器> mysql ... 执行。
"""

import argparse
import os
import sys
from typing import Optional

try:
    import paramiko
except ImportError:
    paramiko = None

# ---------- 与 deploy.py 一致的 SSH 配置 ----------
SERVER = os.getenv("DEPLOY_SERVER", "ubuntu@101.34.64.224")
PASSWORD = os.getenv("DEPLOY_PASSWORD", "Shuzibaba@888")
REMOTE_DIR = os.getenv("DEPLOY_REMOTE_DIR", "/opt/digital-dad")

# ---------- 数据库配置（与 application.yml 一致，执行时在服务器上连接）----------
MYSQL_HOST = os.getenv("MYSQL_HOST", "127.0.0.1")
MYSQL_PORT = os.getenv("MYSQL_PORT", "3306")
MYSQL_USER = os.getenv("MYSQL_USER", "digital_dad")
MYSQL_PASSWORD = os.getenv("MYSQL_PASSWORD", "digital_dad")
MYSQL_DATABASE = os.getenv("MYSQL_DATABASE", "digital_dad")
# MySQL 在 Docker 中时，填容器名或 ID；设置后远程执行 docker exec -i <容器> mysql ...
MYSQL_DOCKER_CONTAINER = os.getenv("MYSQL_DOCKER_CONTAINER", "").strip()

# 允许执行的语句类型（仅 DML）
DML_PREFIXES = ("INSERT", "UPDATE", "DELETE", "REPLACE")
# 禁止执行的类型（DDL 等）
SKIP_PREFIXES = (
    "CREATE", "ALTER", "DROP", "TRUNCATE", "RENAME",
    "GRANT", "REVOKE", "COMMIT", "ROLLBACK",
    "SELECT", "SHOW", "DESCRIBE", "EXPLAIN", "USE",
)


def split_sql(content: str) -> list[str]:
    """按分号拆分 SQL，保留字符串内的分号。"""
    statements = []
    current = []
    in_string = None
    i = 0
    content = content.replace("\r\n", "\n").replace("\r", "\n")

    while i < len(content):
        c = content[i]
        if in_string:
            if c == "\\" and i + 1 < len(content):
                current.append(c)
                current.append(content[i + 1])
                i += 2
                continue
            if (c == "'" and in_string == "'") or (c == '"' and in_string == '"'):
                in_string = None
            current.append(c)
            i += 1
            continue
        if c in ("'", '"'):
            in_string = c
            current.append(c)
            i += 1
            continue
        if c == ";":
            stmt = "".join(current).strip()
            if stmt:
                statements.append(stmt)
            current = []
            i += 1
            continue
        current.append(c)
        i += 1

    if current:
        stmt = "".join(current).strip()
        if stmt:
            statements.append(stmt)
    return statements


def strip_sql_comments(stmt: str) -> str:
    """去掉行首的 -- 和 # 注释、空白。"""
    lines = []
    for line in stmt.split("\n"):
        s = line.strip()
        if s.startswith("--") or s.startswith("#"):
            continue
        lines.append(line)
    return "\n".join(lines).strip()


def get_statement_type(stmt: str) -> Optional[str]:
    """返回语句类型：INSERT/UPDATE/DELETE/REPLACE 或 None。"""
    s = strip_sql_comments(stmt)
    if not s:
        return None
    upper = s.upper()
    for skip in SKIP_PREFIXES:
        if upper.startswith(skip + " ") or upper.startswith(skip + "\t") or upper == skip:
            return None
    for dml in DML_PREFIXES:
        if upper.startswith(dml + " ") or upper.startswith(dml + "\t") or upper.startswith(dml + "\n"):
            return dml
    return None


def filter_dml_statements(content: str, allow_all: bool = False) -> list[tuple[str, str]]:
    """
    解析 SQL 内容，返回 (type, statement) 列表。
    allow_all=True 时不过滤类型，只跳过空和纯注释。
    """
    statements = split_sql(content)
    result = []
    for stmt in statements:
        stmt = stmt.strip()
        if not stmt:
            continue
        kind = None if allow_all else get_statement_type(stmt)
        if allow_all:
            # 随便给个类型用于显示
            u = strip_sql_comments(stmt).upper()
            for p in DML_PREFIXES + ("CREATE", "ALTER", "DROP", "TRUNCATE"):
                if u.startswith(p + " ") or u.startswith(p + "\t") or u.startswith(p + "\n"):
                    kind = p
                    break
            if not kind:
                kind = "OTHER"
            result.append((kind, stmt))
        elif kind:
            result.append((kind, stmt))
    return result


def main():
    parser = argparse.ArgumentParser(
        description="在云端服务器上执行 SQL（仅 DML：INSERT/UPDATE/DELETE/REPLACE）",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    parser.add_argument(
        "sql_files",
        nargs="+",
        metavar="SQL_FILE",
        help="SQL 文件路径，使用 - 表示从 stdin 读取",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="仅解析并列出将要执行的语句，不连接服务器",
    )
    parser.add_argument(
        "--all",
        action="store_true",
        dest="run_all",
        help="执行所有语句（含 DDL），默认仅 DML",
    )
    parser.add_argument(
        "--direct",
        action="store_true",
        help="本机直连 MYSQL_HOST 执行（不经过 SSH），需安装 PyMySQL",
    )
    args = parser.parse_args()

    # 读取 SQL 内容
    all_content = []
    for path in args.sql_files:
        if path == "-":
            all_content.append(sys.stdin.read())
        else:
            if not os.path.isfile(path):
                print(f"[错误] 文件不存在: {path}", file=sys.stderr)
                sys.exit(1)
            with open(path, "r", encoding="utf-8", errors="replace") as f:
                all_content.append(f.read())
    content = "\n\n".join(all_content)

    # 过滤语句
    filtered = filter_dml_statements(content, allow_all=args.run_all)
    if not filtered:
        print("[提示] 未发现可执行的 DML 语句（INSERT/UPDATE/DELETE/REPLACE）。")
        if not args.run_all:
            print("      若需执行 DDL 或全部语句，请使用 --all。")
        sys.exit(0)

    # 统计
    by_type = {}
    for kind, _ in filtered:
        by_type[kind] = by_type.get(kind, 0) + 1
    print(f"共解析出 {len(filtered)} 条可执行语句: {by_type}")

    if args.dry_run:
        for i, (kind, stmt) in enumerate(filtered, 1):
            preview = stmt[:80].replace("\n", " ")
            if len(stmt) > 80:
                preview += "..."
            print(f"  [{i}] {kind}: {preview}")
        print("\n[dry-run] 未连接服务器，退出。")
        return

    # 生成仅含这些语句的 SQL 文件内容
    dml_sql = ";\n\n".join(s for _, s in filtered) + "\n"
    if not dml_sql.strip():
        sys.exit(0)

    if args.direct:
        if pymysql is None:
            print("[错误] --direct 需安装 PyMySQL: pip install pymysql", file=sys.stderr)
            sys.exit(1)
        try:
            conn = pymysql.connect(
                host=MYSQL_HOST,
                port=int(MYSQL_PORT),
                user=MYSQL_USER,
                password=MYSQL_PASSWORD,
                database=MYSQL_DATABASE,
                charset="utf8mb4",
                cursorclass=pymysql.cursors.Cursor,
            )
            print(f"[执行] 直连 {MYSQL_HOST}:{MYSQL_PORT}/{MYSQL_DATABASE} 执行 DML ...")
            with conn.cursor() as cur:
                for i, (kind, stmt) in enumerate(filtered, 1):
                    cur.execute(stmt)
                    print(f"  执行 [{i}/{len(filtered)}] {kind} OK")
            conn.commit()
            conn.close()
            print("[完成] SQL 执行成功。")
        except Exception as e:
            print(f"[错误] 执行失败: {e}", file=sys.stderr)
            sys.exit(1)
        return

    # SSH 连接信息
    if "@" not in SERVER:
        print("[错误] 请设置 DEPLOY_SERVER 为 user@host 格式，或与 deploy.py 一致。", file=sys.stderr)
        sys.exit(1)
    user, host = SERVER.split("@", 1)

    mysql_args = (
        f"--default-character-set=utf8mb4 "
        f"-h{MYSQL_HOST} -P{MYSQL_PORT} -u{MYSQL_USER} -p{MYSQL_PASSWORD} "
        f"{MYSQL_DATABASE}"
    )

    try:
        ssh = paramiko.SSHClient()
        ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        try:
            ssh.connect(host, username=user, password=PASSWORD, timeout=30)
        except Exception as e:
            print(f"[错误] SSH 连接失败: {e}", file=sys.stderr)
            sys.exit(1)

        container_id = MYSQL_DOCKER_CONTAINER
        if not container_id:
            # 自动通过 docker ps | grep mysql 获取容器 ID
            print("[执行] 正在获取 MySQL 容器 ID（docker ps | grep mysql）...")
            stdin_dp, stdout_dp, stderr_dp = ssh.exec_command("docker ps | grep mysql", timeout=10)
            out_dp = stdout_dp.read().decode("utf-8", errors="replace").strip()
            err_dp = stderr_dp.read().decode("utf-8", errors="replace").strip()
            if err_dp and "Cannot connect to the Docker daemon" in err_dp:
                print("[错误] 远程服务器无法连接 Docker，请检查 docker 是否运行。", file=sys.stderr)
                ssh.close()
                sys.exit(1)
            lines = [ln.strip() for ln in out_dp.splitlines() if ln.strip()]
            if not lines:
                print("[错误] 未找到 MySQL 容器（docker ps | grep mysql 无结果）。请确认容器已启动，或设置 MYSQL_DOCKER_CONTAINER=容器名或ID。", file=sys.stderr)
                ssh.close()
                sys.exit(1)
            # docker ps 输出第一列为 CONTAINER ID
            container_id = lines[0].split()[0]
            print(f"[执行] 使用容器: {container_id}")

        mysql_cmd = f"docker exec -i {container_id} mysql {mysql_args}"
        print(f"[执行] 在 {SERVER} 上执行 MySQL（仅 DML）...")
        stdin, stdout, stderr = ssh.exec_command(mysql_cmd, timeout=120)
        stdin.write(dml_sql)
        stdin.channel.shutdown_write()
        err = stderr.read().decode("utf-8", errors="replace")
        out = stdout.read().decode("utf-8", errors="replace")
        exit_code = stdout.channel.recv_exit_status()
        ssh.close()

        if out:
            print(out.rstrip())
        if err:
            print(err.rstrip(), file=sys.stderr)
        if exit_code != 0:
            print(f"[错误] mysql 退出码: {exit_code}", file=sys.stderr)
            sys.exit(1)
        print("[完成] SQL 执行成功。")
    except Exception as e:
        print(f"[错误] 执行失败: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
