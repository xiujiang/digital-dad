-- 排查 PromptSceneScope 枚举不匹配：查看 prompt_scene 表中实际存在的 scope 值
-- 合法枚举值仅限: BOARD_INTERVIEW, BOARD_SUMMARY, BOARD_STORY, DELIVERABLE
SELECT DISTINCT `scope`, COUNT(*) AS cnt
FROM `prompt_scene`
GROUP BY `scope`
ORDER BY `scope`;
