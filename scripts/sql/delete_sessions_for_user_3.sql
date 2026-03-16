-- 删除 user_id=3 的全部会话及相关数据，并重置板块进度（执行测试前使该用户会话干净）
-- 依赖：project_participant.user_id, interview_session.participant_id；material_snapshot 引用 board_summary 且 ON DELETE RESTRICT，故需先删

-- 1. 删除该用户会话下的素材快照（否则删除 session 会因 board_summary 被 material_snapshot 引用而失败）
DELETE m FROM material_snapshot m
INNER JOIN board_summary bs ON m.summary_id = bs.id
INNER JOIN interview_session s ON bs.session_id = s.id
INNER JOIN project_participant pp ON s.participant_id = pp.id
WHERE pp.user_id = 3;

-- 2. 删除该用户作为参与者的所有采访会话（会 CASCADE 删除：conversation_message, session_board_rounds, board_summary, summary_item, board_summary_key_person, board_story, key_person 等）
DELETE s FROM interview_session s
INNER JOIN project_participant pp ON s.participant_id = pp.id
WHERE pp.user_id = 3;

-- 3. 重置该用户的板块进度（my-status 的 isCompleted / currentProjectBoardId 来自 project_participant.current_board_order，不重置则前几板仍显示已完成、当前仍为第 4 板）
UPDATE project_participant
SET current_board_order = 1
WHERE user_id = 3;
