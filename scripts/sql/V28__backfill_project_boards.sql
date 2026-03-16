-- ============================================================
-- 为「创建时未挂载板块」的项目补全默认 4 板块（幂等，与 V18/V20 逻辑一致）
-- 原因：V18/V20 只为迁移时已存在的项目插入 project_board，之后新建的项目依赖本迁移或应用层 ensureDefaultBoardsForNewProject 补全
-- ============================================================

INSERT IGNORE INTO `project_board` (`project_id`, `board_meta_id`, `display_order`, `created_at`)
SELECT p.`id`, bm.`id`, bm.`display_order`, NOW()
FROM `project` p
INNER JOIN `board_meta` bm ON bm.`code` IN ('FAMILY_ORIGIN', 'GROWTH', 'LOVE_STORY', 'FUTURE_PROMISE')
WHERE p.`deleted_at` IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM `project_board` pb
    WHERE pb.`project_id` = p.`id` AND pb.`board_meta_id` = bm.`id`
  );
