-- ============================================================
-- 会话按板块拆分：一个 session 仅对应一个板块 (participant + board)
-- 依赖: V5__session_module.sql
-- ============================================================
-- 1. 将 current_project_board_id 为 NULL 的会话回填为该项目第一个板块（按 display_order）
UPDATE interview_session s
INNER JOIN (
    SELECT is2.id AS session_id,
           (SELECT pb.id FROM project_board pb WHERE pb.project_id = is2.project_id ORDER BY pb.display_order ASC LIMIT 1) AS first_board_id
    FROM interview_session is2
    WHERE is2.current_project_board_id IS NULL
) t ON s.id = t.session_id
SET s.current_project_board_id = t.first_board_id
WHERE t.first_board_id IS NOT NULL;

-- 若仍有 NULL（项目无板块等），删除无效会话以便加 NOT NULL
DELETE FROM interview_session WHERE current_project_board_id IS NULL;

-- 2. 改为 NOT NULL
ALTER TABLE interview_session
    MODIFY COLUMN current_project_board_id BIGINT NOT NULL COMMENT '本会话所属板块(一个会话仅对应一个板块)';

-- 3. 同一参与者同一板块仅允许一个会话
ALTER TABLE interview_session
    ADD UNIQUE KEY uk_participant_board (participant_id, current_project_board_id);
