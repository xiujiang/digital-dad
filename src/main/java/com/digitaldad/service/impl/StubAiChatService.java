package com.digitaldad.project.service.impl;

import com.digitaldad.project.service.AiChatService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 占位实现
 * <p>当 app.ai.enabled=false 或未配置时启用，返回固定的占位文本，用于开发调试。</p>
 */
@Service
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "false", matchIfMissing = true)
public class StubAiChatService implements AiChatService {

    /**
     * 返回基于最后一条用户消息的占位回复
     */
    @Override
    public String chat(String systemPrompt, List<String> userMessages) {
        String last = userMessages.isEmpty() ? "" : userMessages.get(userMessages.size() - 1);
        return "[AI占位] 感谢你的分享。你提到：「" + (last.length() > 50 ? last.substring(0, 50) + "…" : last) + "」。能再具体说说吗？";
    }

    /**
     * 返回固定格式的占位小结 JSON
     */
    @Override
    public String generateSummary(String prompt, String conversation, String boardName) {
        return "{\"title\":\"" + boardName + "小结\",\"key_characters\":[],\"core_points\":[{\"type\":\"事实类\",\"content\":\"[占位]请接入真实AI后重新生成\"}],\"more_details\":[]}";
    }

    /**
     * 返回占位故事文本
     */
    @Override
    public String generateStory(String prompt, String conversation, String boardName) {
        return "[故事占位] 这是一段关于「" + boardName + "」的珍贵记忆。请接入真实AI后重新生成。";
    }

    /**
     * 返回占位交付物文本
     */
    @Override
    public String generateDeliverable(String prompt, String materialsJson, String contentType) {
        return "[交付物占位] " + contentType + " 生成内容。请接入真实AI后重新生成。素材长度：" + (materialsJson != null ? materialsJson.length() : 0);
    }
}
