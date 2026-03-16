-- ============================================================
-- 在 board_meta 就绪后，为已有项目挂上 4 个板块（与 V18 逻辑一致，幂等）
-- 依赖: V2, V3, V19（board_meta 已有 4 条）
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
