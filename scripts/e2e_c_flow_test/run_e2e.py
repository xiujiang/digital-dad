#!/usr/bin/env python3
"""
C 端「从头到生成故事」E2E 测试脚本。

流程：登录 → my-status → 进入板块会话 → 发消息 → 提交 → 创建小结 → 确认小结
     → 下一板块（重复）→ 创建故事 → 获取故事。

参数来源约定：每个接口的入参均来自「上一接口返回」或「初始 CONFIG」：
  - token：来自 1.登录 返回的 data.token
  - project_id：来自 CONFIG（my-status path、POST sessions body）
  - sessionId：来自 3b.进入板块会话 返回的 data.id
  - projectBoardId：来自 my-status 的 data.currentProjectBoardId
  - summaryId：来自 6.创建小结 返回的 data.id
  - 9/10 的 sessionId、projectBoardId：来自循环最后一轮的 3b 与 my-status
  唯一非接口返回：发送消息的 content（测试文案）、CONFIG 的 phone/password/project_id。

使用前：
  1. 启动后端（如 mvn spring-boot:run）
  2. 测试时建议将每板块 AI 对话轮数改为 2：启动时加
     -Dinterview.max_rounds_per_board=2  或  export INTERVIEW_MAX_ROUNDS_PER_BOARD=2
  3. 配置方式二选一：
     - 直接使用 token：设置 TOKEN=xxx（或 CONFIG 中填 TOKEN），跳过登录
     - 登录方式：设置 PHONE、PASSWORD，脚本会先调登录再跑流程
     - 其余：BASE_URL, PROJECT_ID, MAX_BOARDS

运行：python run_e2e.py  或  python3 run_e2e.py
脚本会打印每一次接口调用的完整响应 data。
"""

import os
import sys
import json
import time
from typing import Optional

try:
    import requests
except ImportError:
    print("请先安装 requests: pip install requests")
    sys.exit(1)

# ---------- 配置 ----------
CONFIG = {
    "BASE_URL": os.environ.get("BASE_URL", "http://localhost:8080"),
    "TOKEN": os.environ.get("TOKEN", "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIzIiwicm9sZXMiOlsiSE9TVCIsIlNVUEVSX0FETUlOIl0sInBob25lIjoiMTM2MjM2NDE1MDEiLCJpYXQiOjE3NzM1NTcxNjgsImV4cCI6MTc3NDE2MTk2OH0.nzFpVJLbK5cIDsJP3MnY2-YwNUwwdxtW2h-X5uIvD7lMyPg_7SEYlwda-RIY2XOL"),  # 直接使用 token 时填写，不填则走登录流程（需 PHONE+PASSWORD）
    "PHONE": os.environ.get("PHONE", ""),
    "PASSWORD": os.environ.get("PASSWORD", ""),
    "PROJECT_ID": os.environ.get("PROJECT_ID", "3"),
    "MAX_BOARDS": int(os.environ.get("MAX_BOARDS", "2")),
}

def log(msg: str) -> None:
    print(f"[{time.strftime('%H:%M:%S')}] {msg}")

def print_request(step_name: str, method: str, url: str, json_body: Optional[dict] = None) -> None:
    """打印本次请求：方法、URL、请求体。"""
    log(f">>> 请求 [{step_name}] {method} {url}")
    if json_body is not None and json_body:
        try:
            out = json.dumps(json_body, ensure_ascii=False, indent=2)
            for line in out.splitlines():
                print("  " + line)
        except Exception:
            print("  ", json_body)
    else:
        print("  (无 body)")
    print()

def print_response(step_name: str, status_code: int, body: dict, *, is_ai_step: bool = False) -> None:
    """打印本次完整响应：状态码、code、message、data（含 AI 返回时标注）。"""
    label = "响应 (含 AI 返回)" if is_ai_step else "响应"
    log(f"<<< {label} [{step_name}] HTTP {status_code}")
    if not body:
        print("  (空)")
    else:
        try:
            out = json.dumps(body, ensure_ascii=False, indent=2)
            for line in out.splitlines():
                print("  " + line)
        except Exception:
            print("  ", body)
    print()

def req(method: str, path: str, token: Optional[str] = None, json_body: Optional[dict] = None, print_step: Optional[str] = None, is_ai_step: bool = False) -> dict:
    url = CONFIG["BASE_URL"].rstrip("/") + path
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    step_label = print_step or path
    print_request(step_label, method, url, json_body if method != "GET" else None)
    if method == "GET":
        r = requests.get(url, headers=headers, timeout=30)
    elif method == "POST":
        r = requests.post(url, headers=headers, json=json_body or {}, timeout=60)
    elif method == "PUT":
        r = requests.put(url, headers=headers, json=json_body or {}, timeout=30)
    else:
        raise ValueError(method)
    body = r.json() if r.text else {}
    print_response(step_label, r.status_code, body, is_ai_step=is_ai_step)
    if body.get("code") != 200:
        raise RuntimeError(f"{method} {path} -> {r.status_code} body={body}")
    return body.get("data")

def run():
    base = CONFIG["BASE_URL"].rstrip("/")
    token_from_config = (CONFIG.get("TOKEN") or "").strip()
    phone = CONFIG.get("PHONE") or ""
    password = CONFIG.get("PASSWORD") or ""
    project_id = CONFIG["PROJECT_ID"]
    max_boards = CONFIG["MAX_BOARDS"]

    if not project_id:
        print("请设置环境变量 PROJECT_ID，或在本文件 CONFIG 中填写")
        sys.exit(1)

    if token_from_config:
        log("========== 使用配置的 TOKEN（跳过登录） ==========")
        token = token_from_config
    else:
        if not phone or not password:
            print("请设置 TOKEN，或设置 PHONE 和 PASSWORD 走登录流程")
            sys.exit(1)
        log("========== 1. 登录 ==========")
        login_data = req("POST", "/api/auth/login-password", json_body={"phone": phone, "password": password}, print_step="1.登录")
        if not login_data or not login_data.get("token"):
            raise RuntimeError("登录失败，未返回 token")
        token = login_data["token"]

    log("========== 2. my-status（获取状态与板块） ==========")
    # 参数来源：project_id=CONFIG, token=1.登录
    status = req("GET", f"/api/c/projects/{project_id}/my-status", token=token, print_step="2.my-status")
    step = status.get("step")
    current_board_id = status.get("currentProjectBoardId")
    boards = status.get("boards") or []
    session_id = status.get("sessionId")
    log(f"step={step}, currentProjectBoardId={current_board_id}, boards数量={len(boards)}, sessionId={session_id}")

    if not boards and step != "ALL_COMPLETED":
        raise RuntimeError("项目下无板块，请先在后台为项目配置板块")

    completed_boards = 0
    story_session_id = None
    story_board_id = None

    while completed_boards < max_boards:
        log("========== 3. 进入板块会话 ==========")
        # 参数来源：project_id=CONFIG, token=1.登录
        status = req("GET", f"/api/c/projects/{project_id}/my-status", token=token, print_step="3a.my-status(循环)")
        step = status.get("step")
        current_board_id = status.get("currentProjectBoardId")
        session_id = status.get("sessionId")

        if step == "ALL_COMPLETED":
            log("全部板块已完成，跳出板块循环")
            break

        if not current_board_id:
            raise RuntimeError("my-status 未返回 currentProjectBoardId")

        # 参数来源：project_id=CONFIG, projectBoardId=上一步 my-status.currentProjectBoardId, role 仅未绑定时需要
        # 注意：不传 sessionId。接口为「创建或恢复」：该用户在该板块下已有进行中/待确认会话则返回该会话（恢复），否则新建。sessionId 始终来自本响应。
        body = {"projectId": int(project_id), "projectBoardId": current_board_id}
        if step == "NOT_BOUND":
            body["role"] = "GROOM"
        session = req("POST", "/api/c/sessions", token=token, json_body=body, print_step="3b.进入板块会话")
        session_id = session.get("id")  # 后续步骤的 sessionId 均来自本响应（新建或已有会话）
        if not session_id:
            raise RuntimeError("POST /api/c/sessions 未返回 session id")
        log(f"进入板块会话 sessionId={session_id}, currentProjectBoardId={session.get('currentProjectBoardId')}")

        log("========== 4. 发送消息 ==========")
        # 参数来源：sessionId=3b.进入板块会话.id, token=1.登录；content 为测试文案（真实场景为用户输入）
        msg = req("POST", f"/api/c/sessions/{session_id}/messages", token=token,
                  json_body={"content": f"我很喜欢我现在的家庭，我挺爱他们的"}, print_step="4.发送消息")
        log(f"消息已发送 messageId={msg.get('id')}")

        log("========== 5. 提交会话（触发 AI 回复） ==========")
        submit_result = req("POST", f"/api/c/sessions/{session_id}/submit", token=token, print_step="5.提交会话", is_ai_step=True)
        log(f"提交成功 roundCount={submit_result.get('roundCount')}, newMessages 数量={len(submit_result.get('newMessages') or [])}")

        log("========== 6. 创建小结（生成即确认，默认测试数据不请求 AI；传 useTestData=true 则用真实 AI） ==========")
        summary = req("POST", f"/api/c/sessions/{session_id}/summaries?useTestData=true", token=token, print_step="6.创建小结", is_ai_step=True)
        summary_id = summary.get("id")
        if not summary_id:
            raise RuntimeError("创建小结未返回 id")
        log(f"小结已创建并确认 summaryId={summary_id}，本板块完成")

        story_session_id = session_id
        story_board_id = current_board_id
        completed_boards += 1

        if completed_boards >= max_boards:
            log(f"已达最大板块数 {max_boards}，停止继续下一板块")
            break

        log("========== 7. 再次 my-status（下一板块） ==========")
        time.sleep(0.5)
        status = req("GET", f"/api/c/projects/{project_id}/my-status", token=token, print_step="8.my-status(下一板块)")
        next_step = status.get("step")
        next_board = status.get("currentProjectBoardId")
        log(f"下一轮 step={next_step}, currentProjectBoardId={next_board}")
        if next_step == "ALL_COMPLETED":
            break

    if not story_session_id or story_board_id is None:
        log("未完成任何板块，跳过创建/获取故事")
        return

    log("========== 8. 创建故事（基于最后一板块会话） ==========")
    # 参数来源：sessionId=循环中最后一轮 3b 的 session_id，token=1.登录
    story = req("POST", f"/api/c/sessions/{story_session_id}/stories", token=token, print_step="8.创建故事", is_ai_step=True)
    log(f"故事已创建 storyId={story.get('id')}, content 长度={len(story.get('content') or '')}")

    log("========== 10. 获取故事（按会话+板块） ==========")
    # 参数来源：sessionId=循环最后一轮 3b，projectBoardId=循环最后一轮 my-status.currentProjectBoardId
    story_get = req("GET", f"/api/c/sessions/{story_session_id}/stories?projectBoardId={story_board_id}", token=token, print_step="10.获取故事")
    if story_get:
        log(f"获取故事成功: id={story_get.get('id')}, boardName={story_get.get('boardName')}")
    else:
        log("该会话+板块暂无故事记录（接口返回 null）")

    log("========== E2E 通过 ==========")

if __name__ == "__main__":
    run()
