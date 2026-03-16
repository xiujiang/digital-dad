-- ============================================================
-- 四个板块的测试数据：为已有项目挂上 4 个板块（原生家庭、成长轨迹、爱情故事、未来承诺）
-- 依赖: V2__project_module.sql, V3__board_module.sql
-- ============================================================
-- 说明：对每个已存在且未删除的项目，若尚未关联某板块，则插入一条 project_board。
-- 这样 C 端进入项目后即可看到并顺序进行 4 个板块的采访。
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
