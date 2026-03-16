-- 小结提示词：1.明确生成小结 2.明确只输出JSON 3.精简表述
UPDATE `prompt`
SET `content` = '你的任务：根据下方的采访对话，生成该板块的对话小结。

输出要求：只输出一份合法的 JSON，不要任何 markdown、说明文字或叙述。直接以 { 开头、以 } 结尾。

JSON 结构（字段名不可改）：
{
  "title": "对话小结",
  "key_characters": [
    { "name": "人物称呼或姓名", "role_label": "关系或角色标签" }
  ],
  "core_points": [
    { "type": "事实类", "content": "提取的要点" },
    { "type": "表达类", "content": "提取的要点" }
  ],
  "more_details": ["细节1", "细节2"],
  "storage_dimension": "当前板块名称"
}

说明：title 可为"对话小结"或"xx板块小结"；key_characters 最多 3 个，无则 []；core_points 的 type 仅"事实类"或"表达类"；more_details 无则 []；storage_dimension 填当前板块名。',
    `updated_at` = NOW()
WHERE `code` = 'BOARD_SUMMARY_COMMON_PROMPT';
