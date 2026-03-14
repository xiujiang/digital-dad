-- ============================================================
-- 为现有全部用户增加超管角色（SUPER_ADMIN）
-- 执行前请确认：仅在有需要的环境执行，避免全员变超管
-- ============================================================

-- 为所有未删除、且尚未拥有 SUPER_ADMIN 角色的用户插入一条 user_role 记录
INSERT INTO `user_role` (`user_id`, `role`, `created_at`)
SELECT u.`id`, 'SUPER_ADMIN', NOW()
FROM `user` u
WHERE u.`deleted_at` IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM `user_role` ur
    WHERE ur.`user_id` = u.`id` AND ur.`role` = 'SUPER_ADMIN'
  );
