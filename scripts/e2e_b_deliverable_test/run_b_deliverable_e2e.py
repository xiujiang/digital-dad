#!/usr/bin/env python3
"""
B 端「交付物生成」E2E 测试脚本。

逻辑：
  - 若新郎、新娘都有已确认素材：生成「开场白 + 新郎誓词 + 新娘誓词」
  - 若只有一方有素材：仅生成该方的誓词（新郎誓词或新娘誓词）

接口：POST /api/projects/{projectId}/deliverables/generate，body: {"contentType": "OPENING_SPEECH"|"GROOM_VOW"|"BRIDE_VOW"}
前置：项目下已有 material_snapshot（C 端完成「创建小结并确认」后产生）；当前用户为项目主持人或超管。

使用前：
  1. 启动后端（如 mvn spring-boot:run）
  2. 配置：TOKEN 或 PHONE+PASSWORD；PROJECT_ID、BASE_URL

运行：python run_b_deliverable_e2e.py
"""

import os
import sys
import json
import time
from typing import Optional, Tuple, Any

try:
    import requests
except ImportError:
    print("请先安装 requests: pip install requests")
    sys.exit(1)

# ---------- 配置 ----------
CONFIG = {
    "BASE_URL": os.environ.get("BASE_URL", "http://localhost:8080"),
    "TOKEN": os.environ.get("TOKEN", "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIzIiwicm9sZXMiOlsiSE9TVCIsIlNVUEVSX0FETUlOIl0sInBob25lIjoiMTM2MjM2NDE1MDEiLCJpYXQiOjE3NzM1NTcxNjgsImV4cCI6MTc3NDE2MTk2OH0.nzFpVJLbK5cIDsJP3MnY2-YwNUwwdxtW2h-X5uIvD7lMyPg_7SEYlwda-RIY2XOL"),
    "PHONE": os.environ.get("PHONE", ""),
    "PASSWORD": os.environ.get("PASSWORD", ""),
    "PROJECT_ID": os.environ.get("PROJECT_ID", "3"),
}

def log(msg: str) -> None:
    print(f"[{time.strftime('%H:%M:%S')}] {msg}")


def print_request(step_name: str, method: str, url: str, json_body: Optional[dict] = None) -> None:
    log(f">>> 请求 [{step_name}] {method} {url}")
    if json_body is not None and json_body:
        try:
            for line in json.dumps(json_body, ensure_ascii=False, indent=2).splitlines():
                print("  " + line)
        except Exception:
            print("  ", json_body)
    else:
        print("  (无 body)")
    print()


def print_response(step_name: str, status_code: int, body: dict, *, is_ai_step: bool = False) -> None:
    label = "响应 (含 AI 返回)" if is_ai_step else "响应"
    log(f"<<< {label} [{step_name}] HTTP {status_code}")
    if not body:
        print("  (空)")
    else:
        try:
            for line in json.dumps(body, ensure_ascii=False, indent=2).splitlines():
                print("  " + line)
        except Exception:
            print("  ", body)
    print()


def req(
    method: str,
    path: str,
    token: Optional[str] = None,
    json_body: Optional[dict] = None,
    print_step: Optional[str] = None,
    is_ai_step: bool = False,
) -> dict:
    """发起请求，code!=200 时抛异常。"""
    url = CONFIG["BASE_URL"].rstrip("/") + path
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    step_label = print_step or path
    print_request(step_label, method, url, json_body if method != "GET" else None)
    if method == "GET":
        r = requests.get(url, headers=headers, timeout=30)
    elif method == "POST":
        r = requests.post(url, headers=headers, json=json_body or {}, timeout=120)
    else:
        raise ValueError(method)
    body = r.json() if r.text else {}
    print_response(step_label, r.status_code, body, is_ai_step=is_ai_step)
    if body.get("code") != 200:
        raise RuntimeError(f"{method} {path} -> {r.status_code} body={body}")
    return body.get("data")


def req_may_fail(
    method: str,
    path: str,
    token: str,
    json_body: Optional[dict],
    print_step: str,
    is_ai_step: bool = False,
) -> Tuple[bool, Any, dict]:
    """发起请求，返回 (是否成功, data, 完整 body)，不抛异常。"""
    url = CONFIG["BASE_URL"].rstrip("/") + path
    headers = {"Content-Type": "application/json", "Authorization": f"Bearer {token}"}
    print_request(print_step, method, url, json_body if method != "GET" else None)
    r = requests.post(url, headers=headers, json=json_body or {}, timeout=120)
    body = r.json() if r.text else {}
    print_response(print_step, r.status_code, body, is_ai_step=is_ai_step)
    ok = body.get("code") == 200
    return ok, body.get("data"), body


def run() -> None:
    token_from_config = (CONFIG.get("TOKEN") or "").strip()
    phone = CONFIG.get("PHONE") or ""
    password = CONFIG.get("PASSWORD") or ""
    project_id = CONFIG["PROJECT_ID"]

    if not project_id:
        print("请设置环境变量 PROJECT_ID 或在 CONFIG 中填写")
        sys.exit(1)

    if token_from_config:
        log("========== 使用配置的 TOKEN（跳过登录） ==========")
        token = token_from_config
    else:
        if not phone or not password:
            print("请设置 TOKEN，或设置 PHONE 和 PASSWORD 走登录流程")
            sys.exit(1)
        log("========== 1. 登录 ==========")
        login_data = req(
            "POST",
            "/api/auth/login-password",
            json_body={"phone": phone, "password": password},
            print_step="1.登录",
        )
        if not login_data or not login_data.get("token"):
            raise RuntimeError("登录失败，未返回 token")
        token = login_data["token"]

    log("========== 2. 尝试生成开场白（需新郎+新娘均有素材） ==========")
    ok_open, data_open, _ = req_may_fail(
        "POST",
        f"/api/projects/{project_id}/deliverables/generate",
        token,
        {"contentType": "OPENING_SPEECH"},
        print_step="2.生成开场白",
        is_ai_step=True,
    )

    if ok_open:
        log("开场白生成成功 → 新郎、新娘均有素材，继续生成新郎誓词、新娘誓词")
        log("========== 3a. 生成新郎誓词 ==========")
        req(
            "POST",
            f"/api/projects/{project_id}/deliverables/generate",
            token=token,
            json_body={"contentType": "GROOM_VOW"},
            print_step="3a.生成新郎誓词",
            is_ai_step=True,
        )
        log("========== 3b. 生成新娘誓词 ==========")
        req(
            "POST",
            f"/api/projects/{project_id}/deliverables/generate",
            token=token,
            json_body={"contentType": "BRIDE_VOW"},
            print_step="3b.生成新娘誓词",
            is_ai_step=True,
        )
        log("========== B 端交付物 E2E 通过（开场白 + 新郎誓词 + 新娘誓词） ==========")
        return

    log("开场白未生成（可能仅一方有素材或双方都无）→ 分别尝试新郎誓词、新娘誓词")
    log("========== 4a. 尝试生成新郎誓词 ==========")
    ok_groom, _, _ = req_may_fail(
        "POST",
        f"/api/projects/{project_id}/deliverables/generate",
        token,
        {"contentType": "GROOM_VOW"},
        print_step="4a.生成新郎誓词",
        is_ai_step=True,
    )
    log("========== 4b. 尝试生成新娘誓词 ==========")
    ok_bride, _, _ = req_may_fail(
        "POST",
        f"/api/projects/{project_id}/deliverables/generate",
        token,
        {"contentType": "BRIDE_VOW"},
        print_step="4b.生成新娘誓词",
        is_ai_step=True,
    )

    if ok_groom or ok_bride:
        parts = []
        if ok_groom:
            parts.append("新郎誓词")
        if ok_bride:
            parts.append("新娘誓词")
        log(f"========== B 端交付物 E2E 通过（仅生成: {' + '.join(parts)}） ==========")
    else:
        log("========== 新郎、新娘均无已确认素材，未生成任何交付物 ==========")
        log("请先跑 C 端流程完成「创建小结并确认」，或确保项目下存在 material_snapshot 数据。")


if __name__ == "__main__":
    run()
