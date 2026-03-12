package com.digitaldad.project.service;

import com.digitaldad.ai.dto.ChatMessage;

import java.util.List;

/**
 * AI 对话服务接口
 * <p>定义与大模型交互的对话、小结生成、故事生成、交付物生成等能力。可接入 DeepSeek 等实现或使用 Stub 占位实现。</p>
 */
public interface AiChatService {

    /**
     * 简单对话（仅用户消息，无历史上下文）
     *
     * @param systemPrompt 系统提示词
     * @param userMessages 用户消息列表
     * @return AI 回复内容
     */
    String chat(String systemPrompt, List<String> userMessages);

    /**
     * 带历史上下文的对话
     *
     * @param systemPrompt 系统提示词
     * @param messages     包含 role、content 的消息列表
     * @return AI 回复内容
     */
    default String chatWithHistory(String systemPrompt, List<ChatMessage> messages) {
        List<String> userOnly = messages.stream()
                .filter(m -> "user".equalsIgnoreCase(m.getRole()))
                .map(ChatMessage::getContent)
                .filter(c -> c != null && !c.isBlank())
                .toList();
        return chat(systemPrompt, userOnly);
    }

    /**
     * 根据对话内容生成板块小结（JSON 格式）
     *
     * @param prompt       提示词
     * @param conversation 对话全文
     * @param boardName    板块名称
     * @return 小结 JSON 字符串
     */
    String generateSummary(String prompt, String conversation, String boardName);

    /**
     * 根据对话内容生成故事叙述
     *
     * @param prompt       提示词
     * @param conversation 对话全文
     * @param boardName    板块名称
     * @return 故事文本
     */
    String generateStory(String prompt, String conversation, String boardName);

    /**
     * 根据素材 JSON 生成指定类型的交付物
     *
     * @param prompt       提示词
     * @param materialsJson 已确认素材的 JSON
     * @param contentType  内容类型（如开场白、誓言等）
     * @return 交付物内容
     */
    String generateDeliverable(String prompt, String materialsJson, String contentType);
}
