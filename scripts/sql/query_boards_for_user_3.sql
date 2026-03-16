-- 查询 user_id=3 参与了哪些板块（从会话与小结数据看）
-- 1) 参与者当前进度
-- 2) 该用户所有会话对应的板块（参与过的板块）
-- 3) 已生成小结的板块（已完成的板块）

-- 1. 参与者信息与当前板块进度
SELECT pp.id AS participant_id, pp.project_id, pp.user_id, pp.role_type, pp.status AS participant_status,
       pp.current_board_order,
       pb.id AS current_project_board_id, bm.code AS current_board_code, bm.name AS current_board_name
FROM project_participant pp
LEFT JOIN project_board pb ON pb.project_id = pp.project_id AND pb.display_order = pp.current_board_order
LEFT JOIN board_meta bm ON bm.id = pb.board_meta_id
WHERE pp.user_id = 3;

-- 2. 该用户参与过的所有板块（按会话：每个 session 对应一个板块）
SELECT s.id AS session_id, s.project_id, s.current_project_board_id AS project_board_id,
       bm.code AS board_code, bm.name AS board_name,
       s.status AS session_status, s.round_count, s.created_at
FROM interview_session s
INNER JOIN project_participant pp ON s.participant_id = pp.id
INNER JOIN project_board pb ON s.current_project_board_id = pb.id
INNER JOIN board_meta bm ON pb.board_meta_id = bm.id
WHERE pp.user_id = 3
ORDER BY pp.project_id, pb.display_order, s.created_at;

-- 3. 已生成小结的板块（说明该板块已完成）
SELECT bs.id AS summary_id, bs.session_id, bs.project_board_id,
       bm.code AS board_code, bm.name AS board_name,
       bs.status AS summary_status, bs.title, bs.generated_at
FROM board_summary bs
INNER JOIN interview_session s ON bs.session_id = s.id
INNER JOIN project_participant pp ON s.participant_id = pp.id
INNER JOIN project_board pb ON bs.project_board_id = pb.id
INNER JOIN board_meta bm ON pb.board_meta_id = bm.id
WHERE pp.user_id = 3
ORDER BY pp.project_id, pb.display_order, bs.generated_at;
