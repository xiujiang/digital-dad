-- 查询项目 3 的参与者（用于核对 C 端入口 roleOptions 的「是否绑定」）
-- 接口 GET /api/c/entry/{shareToken} 的 roleOptions.available = 未存在该角色则 true，已存在则 false

SELECT
    pp.id AS participant_id,
    pp.project_id,
    pp.user_id,
    pp.role_type,
    pp.status,
    u.name  AS user_name,
    u.phone AS user_phone
FROM project_participant pp
LEFT JOIN user u ON u.id = pp.user_id
WHERE pp.project_id = 3
ORDER BY pp.role_type;

-- 若上面无记录：项目 3 尚无参与者，新郎/新娘都 available=true。
-- 若有 GROOM：新郎已绑定，available=false；若有 BRIDE：新娘已绑定，available=false。
