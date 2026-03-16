-- ============================================================
-- 1. 查询当前会话信息（含项目、参与者、板块）
-- 可按需在末尾加: AND pp.user_id = 3
-- ============================================================
SELECT s.id AS session_id,
       s.project_id,
       s.participant_id,
       pp.user_id,
       pp.role_type,
       s.current_project_board_id AS project_board_id,
       bm.code AS board_code,
       bm.name AS board_name,
       s.status AS session_status,
       s.round_count,
       s.started_at,
       s.last_active_at,
       s.created_at
FROM interview_session s
INNER JOIN project_participant pp ON s.participant_id = pp.id
INNER JOIN project_board pb ON s.current_project_board_id = pb.id
INNER JOIN board_meta bm ON pb.board_meta_id = bm.id
ORDER BY s.created_at DESC;

-- ============================================================
-- 2. 查询当前小结信息（含会话、板块、条目数）
-- 可按需在末尾加: AND pp.user_id = 3
-- ============================================================
SELECT bs.id AS summary_id,
       bs.session_id,
       bs.participant_id,
       pp.user_id,
       bs.project_id,
       bs.project_board_id,
       bm.code AS board_code,
       bm.name AS board_name,
       bs.version_no,
       bs.status AS summary_status,
       bs.title,
       bs.generated_at,
       bs.confirmed_at,
       bs.created_at,
       (SELECT COUNT(*) FROM summary_item si WHERE si.summary_id = bs.id) AS item_count
FROM board_summary bs
INNER JOIN project_participant pp ON bs.participant_id = pp.id
INNER JOIN project_board pb ON bs.project_board_id = pb.id
INNER JOIN board_meta bm ON pb.board_meta_id = bm.id
ORDER BY bs.created_at DESC;

-- ============================================================
-- 3. 查询当前故事信息（含会话、板块）
-- 可按需在末尾加: AND pp.user_id = 3
-- ============================================================
SELECT st.id AS story_id,
       st.session_id,
       st.participant_id,
       pp.user_id,
       st.project_id,
       st.project_board_id,
       bm.code AS board_code,
       bm.name AS board_name,
       st.version_no,
       LEFT(st.content, 200) AS content_preview,
       st.created_at
FROM board_story st
INNER JOIN project_participant pp ON st.participant_id = pp.id
INNER JOIN project_board pb ON st.project_board_id = pb.id
INNER JOIN board_meta bm ON pb.board_meta_id = bm.id
ORDER BY st.created_at DESC;
